package httpsmanager.docker;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.pmw.tinylog.Logger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.LogConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Volume;

import github.soltaufintel.amalia.web.config.AppConfig;
import httpsmanager.base.FileService;
import httpsmanager.domain.Domain;
import httpsmanager.domain.DomainAccess;

public abstract class AbstractDocker {
    // TODO Docker- und nginx/certbot-Logik voneinander trennen
    private final AppConfig config = new AppConfig();
    private final DockerClient docker;
    
    public AbstractDocker() {
        docker = createClient();
    }
    
    protected abstract DockerClient createClient();
    
    public List<String> getContainerNames(boolean all) {
        return getContainers(all).stream().map(c -> {
            if (c.getNames() != null && c.getNames().length > 0) {
                String ret = c.getNames()[0];
                if (ret.startsWith("/")) {
                    ret = ret.substring(1);
                }
                return ret + ", " + c.getState();
            }
            return "unknown";
        }).sorted().toList();
    }

    public List<Container> getContainers(boolean all) {
        return docker.listContainersCmd().withShowAll(all).exec();
    }

    public void deleteWebContainer() {
        try {
            String name = config.get("d.web-container", "web");
            Logger.debug("Removing web container | config.get(\"d.web-container\", \"web\"): " + name);
            docker.removeContainerCmd(name).withForce(Boolean.TRUE).exec();
            Logger.debug("Removing web container | force container removal: " + name + " => ok");
        } catch (Exception e) {
            Logger.debug("Error removing web container: " + e.getMessage());
        }
    }

    public void deleteCertbotContainer() {
        try {
            String name = config.get("d.certbot-container", "certbot");
            Logger.debug("Removing certbot container | config.get(\"d.certbot-container\", \"web\"): " + name);
            docker.removeContainerCmd(name).withForce(Boolean.TRUE).exec();
            Logger.debug("Removing certbot container | force container removal: " + name + " => ok");
        } catch (Exception e) {
            Logger.debug("Error removing certbot container: " + e.getMessage());
        }
    }
    
    private void pull(String image) {
        try {
        	if (!image.contains(":")) {
        		image += ":latest";
        	}
        	Logger.debug("pulling image: " + image);
            docker.pullImageCmd(image).exec(new PullImageResultCallback()).awaitCompletion();
        	Logger.debug("pulling image: " + image + " => ok");
        } catch (Exception e) {
        	Logger.debug("Error pulling image: " + image);
            Logger.error(e);
        }
    }

    public void startWebContainer() {
        String image = config.get("d.web-image", "nginx");
        String name = config.get("d.web-container", "web");
        
        pull(image);
        
        ExposedPort eighty = ExposedPort.tcp(80);
        ExposedPort p443 = ExposedPort.tcp(443);
        HostConfig hc = new HostConfig()
                .withPortBindings(
                        new PortBinding(Binding.bindPort(80), eighty),
                        new PortBinding(Binding.bindPort(443), p443))
                .withRestartPolicy(RestartPolicy.alwaysRestart())
                .withLogConfig(new LogConfig(LogConfig.LoggingType.DEFAULT, getLogConfig()));
        Logger.debug("- preparing port 80 and 443, restart always, log-config");
        
        String network = config.get("d.web-network");
        if (network != null && !network.isBlank()) {
        	hc = hc.withNetworkMode(network);
        	Logger.debug("- with network: " + network);
        }
        
        List<Bind> binds = new ArrayList<>();
        addBind("d.web", "/usr/share/nginx/html", binds);
        addBind("d.default.conf", "/etc/nginx/conf.d/default.conf", binds);
        hc.withBinds(addCertbotBinds(binds));

        docker.createContainerCmd(image)
            .withExposedPorts(eighty, p443)
            .withName(name)
            .withHostConfig(hc)
            .exec();
        Logger.debug("- container created [from config d.web-container]: " + name + " | image [from config d.web-image]: " + image);
        
        docker.startContainerCmd(name).exec();
        Logger.debug("- container started: " + name);
    }
    
    private void addBind(String configName, String volumePath, List<Bind> binds) {
    	String a = config.get(configName);
        binds.add(new Bind(a, new Volume(volumePath), AccessMode.ro));
		Logger.debug("- volume: [from config " + configName + "] " + a + " -> " + volumePath + " (read-only)");
    }

    private Map<String, String> getLogConfig() {
        Map<String, String> ret = new HashMap<>();
        ret.put("max-size", "5m");
        ret.put("max-file", "5");
        return ret;
    }

    private List<Bind> addCertbotBinds(List<Bind> binds) {
        binds.add(new Bind(config.get("d.certbot") + "/conf", new Volume("/etc/letsencrypt"), AccessMode.rw));
        binds.add(new Bind(config.get("d.certbot") + "/www", new Volume("/var/www/certbot"), AccessMode.rw));
        binds.add(new Bind("certbot_lib_letsencrypt", new Volume("/var/lib/letsencrypt"), AccessMode.rw));
        return binds;
    }
    
    public void startCertbotContainer() {
        String image = certbotImage();
        String name = config.get("d.certbot-container", "certbot");
        
        pull(image);
        
        String mail = config.get("d.mail", config.get("user.mail"));
        String domains = new DomainAccess().list().stream().map(i -> " -d " + i.getPublicDomain()).collect(Collectors.joining());
        String cmd = "certonly --email " + mail + " --webroot -w /var/www/certbot --force-renewal --agree-tos --non-interactive" + domains;
        Logger.info(cmd);

        docker.createContainerCmd(image)
            .withCmd(cmd.split(" "))
            .withName(name)
            .withHostConfig(new HostConfig().withBinds(addCertbotBinds(new ArrayList<>())))
            .exec();
        Logger.debug("- container created [from config d.certbot-container]: " + name + " | image [from config d.certbot-image]: " + image);
    
        docker.startContainerCmd(name).exec();
        Logger.debug("- container started: " + name);
    }

    public String certbotImage() {
        return config.get("d.certbot-image", "certbot/certbot");
    }
    
    /**
     * @param phase 0: only http phase, 1: creating certificates phase, 2: https phase
     */
    public void writeDefaultConf(int phase) {
        StringBuilder sb = new StringBuilder();
        List<Domain> domains = new DomainAccess().list();
        write(phase, domains, true, sb);
        write(phase, domains, false, sb);

        File file = getDefaultConfFile();
        FileService.saveTextFile(file, sb.toString());
        Logger.info("Nginx configuration file updated: " + file.toString());
    }
    
    private void write(int phase, List<Domain> domains, boolean root, StringBuilder sb) {
        domains.stream().filter(d -> d.isRoot() == root).forEach(d -> sb.append(
                getDefaultConfText(phase, d)
                .replace("$publicDomain", d.getPublicDomain())
                .replace("$internalDomain", d.getInternalDomain())
                .replace("$certificateName", d.getCertificateName())
                + "\n"));
    }

    private String getDefaultConfText(int phase, Domain d) {
        // TODO vermutlich muss ich diese Texte noch änderbar machen
        if (phase == 0) {
            if (d.isRoot()) {
                return """
                        server {
                          listen      80;
                          server_name localhost;
                          index index.html index.htm;
                          location / {
                            $internalDomain;
                          }
                        }
                        """;
            } else {
                return """
                        server {
                          listen      80;
                          server_name $publicDomain;
                          location / {
                            proxy_pass http://$internalDomain;
                          }
                        }
                        """;
            }
        } else if (phase == 1) {
            if (d.isRoot()) {
                return """
                        server {
                          listen      80;
                          server_name localhost;
                          index index.html index.htm;
                          location / {
                            $internalDomain;
                          }
                          location ~ /.well-known/acme-challenge/ {
                            root /var/www/certbot;
                          }
                        }
                        """;
            } else {
                return """
                        server {
                          listen      80;
                          server_name $publicDomain;
                          location / {
                            proxy_pass http://$internalDomain;
                          }
                          location ~ /.well-known/acme-challenge/ {
                            root /var/www/certbot;
                          }
                        }
                        """;
            }
        } else if (phase == 2) {
            if (d.isRoot()) {
                return """
                        server {
                          listen      80;
                          server_name localhost;
                          location ~ /.well-known/acme-challenge/ {
                            root /var/www/certbot;
                          }
                          return 301 https://$publicDomain$request_uri;
                        }
                        server {
                          listen      443 ssl http2;
                          ssl_certificate     /etc/letsencrypt/live/$certificateName/fullchain.pem;
                          ssl_certificate_key /etc/letsencrypt/live/$certificateName/privkey.pem;
                          server_name $publicDomain;
                          index index.html index.htm;
                          location / {
                            $internalDomain;
                          }
                          location ~ /.well-known/acme-challenge/ {
                            root /var/www/certbot;
                          }
                        }
                        """;
            } else {
                return """
                        server {
                          listen      80;
                          server_name $publicDomain;
                          location ~ /.well-known/acme-challenge/ {
                            root /var/www/certbot;
                          }
                          return 301 https://$publicDomain$request_uri;
                        }
                        server {
                          listen      443 ssl http2;
                          ssl_certificate     /etc/letsencrypt/live/$certificateName/fullchain.pem;
                          ssl_certificate_key /etc/letsencrypt/live/$certificateName/privkey.pem;
                          server_name $publicDomain;
                          location / {
                            proxy_pass http://$internalDomain;
                          }
                          location ~ /.well-known/acme-challenge/ {
                            root /var/www/certbot;
                          }
                        }
                        """;
            }
        } else {
            throw new RuntimeException("Unsupported phase");
        }
    }

    private File getDefaultConfFile() {
        String dn = config.get("d.default.conf").replace("\\", "/");
        int o = dn.lastIndexOf("/");
        dn = dn.substring(o + 1); // should be "default.conf"
        File file = new File(config.get("save-folder"), dn);
        return file;
    }
    
    public String runCertbot(String cmd) {
        String image = certbotImage();
        String id = docker.createContainerCmd(image)
            .withCmd(cmd.split(" "))
            .withHostConfig(new HostConfig().withBinds(addCertbotBinds(new ArrayList<>())))
            .exec().getId();
        Logger.info("created " + image + " container " + id);
        
        docker.startContainerCmd(id).exec();

        String logs = "";
        for (int i = 1; i <= 6; i++) {
            try {
                Thread.sleep(2 * 1000);
            } catch (Exception e) {
            }
            logs = logs(id);
            if (logs != null && !logs.isEmpty()) {
                break;
            }
        }
        
        new Thread(() -> {
            try {
                Thread.sleep(15 * 60 * 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            String log = logs(id);
            Logger.info("`certbot " + cmd + "` output after 15 minutes before removal: " + log);

            docker.removeContainerCmd(id).withForce(Boolean.TRUE).exec();
            Logger.info("cleanup: " + image + " container removed: " + id);
            
            if ("renew".equals(cmd) && !log.contains("not yet due for renewal")) {
                // Restart nginx
                String nginxContainerName = new AppConfig().get("d.web-container", "web");
                if (nginxContainerName != null && !nginxContainerName.isBlank()) {
                    Logger.info("docker restart " + nginxContainerName);
                    docker.restartContainerCmd(nginxContainerName).exec();
                }
            }
        }).start();
        
        return logs;
    }
    
    public String logs(String container) {
        StringBuffer sb = new StringBuffer();
        try {
            LogContainerCmd logContainerCmd = docker.logContainerCmd(container);
            logContainerCmd.withStdOut(true).withStdErr(true);
            logContainerCmd.exec(new ResultCallback.Adapter<Frame>() {
                @Override
                public void onNext(Frame item) {
                    sb.append(item.toString());
                    sb.append("\n");
                }
            }).awaitCompletion(10, TimeUnit.SECONDS);
            return sb.toString();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void rmf(String container) {
        try {
            docker.removeContainerCmd(container).withForce(Boolean.TRUE).exec();
        } catch (Exception e) {
            Logger.error(e, "Error deleting container: " + container);
        }
    }
}
