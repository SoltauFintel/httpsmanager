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
            docker.removeContainerCmd(name).withForce(Boolean.TRUE).exec();
        } catch (Exception e) {
            Logger.debug("Error removing web container: " + e.getMessage());
        }
    }

    public void deleteCertbotContainer() {
        try {
            String name = config.get("d.certbot-container", "certbot");
            docker.removeContainerCmd(name).withForce(Boolean.TRUE).exec();
        } catch (Exception e) {
            Logger.debug("Error removing certbot container: " + e.getMessage());
        }
    }
    
    private void pull(String image) {
        try {
            docker.pullImageCmd(image).exec(new PullImageResultCallback()).awaitCompletion();
        } catch (Exception e) {
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
        
        List<Bind> binds = new ArrayList<>();
        binds.add(new Bind(config.get("d.web"), new Volume("/usr/share/nginx/html"), AccessMode.ro));
        binds.add(new Bind(config.get("d.default.conf"), new Volume("/etc/nginx/conf.d/default.conf"), AccessMode.ro));
        hc.withBinds(addCertbotBinds(binds));

        docker.createContainerCmd(image)
            .withExposedPorts(eighty, p443)
            .withName(name)
            .withHostConfig(hc)
            .exec();
        
        docker.startContainerCmd(name).exec();
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
    
        docker.startContainerCmd(name).exec();
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
        try {
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
                    Thread.sleep(10 * 60 * 1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                docker.removeContainerCmd(id).withForce(Boolean.TRUE).exec();
                Logger.info("cleanup: " + image + " container gelöscht: " + id);
            }).start();
            
            return logs;
        } catch (Exception e) {
            Logger.error(e);
            return e.getMessage();
        }
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
