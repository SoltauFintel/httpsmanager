package httpsmanager.docker;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.pmw.tinylog.Logger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
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
    private final AppConfig config = new AppConfig();
    private final DockerClient docker;
    
    public AbstractDocker() {
        docker = createClient();
    }
    
    protected abstract DockerClient createClient();
    
    public List<String> getContainerNames() {
        Logger.info("docker ps ...");
        List<Container> r = docker.listContainersCmd().withShowAll(false).exec();
        return r.stream().map(c -> {
            if (c.getNames() != null && c.getNames().length > 0) {
                String ret = c.getNames()[0];
                if (ret.startsWith("/")) {
                    ret = ret.substring(1);
                }
                return ret;
            }
            return "unknown";
        }).sorted().toList();
    }
    
    public void deleteWebContainer() {
        try {
            String name = config.get("d.web-container", "web");
            docker.removeContainerCmd(name).withForce(Boolean.TRUE).exec();
        } catch (Exception e) {
            Logger.error("Error removing web container: " + e.getMessage());
        }
    }

    public void deleteCertbotContainer() {
        try {
            String name = config.get("d.certbot-container", "certbot");
            docker.removeContainerCmd(name).withForce(Boolean.TRUE).exec();
        } catch (Exception e) {
            Logger.error("Error removing certbot container: " + e.getMessage());
        }
    }

    public void startWebContainer() {
        String image = config.get("d.web-image", "nginx");
        String name = config.get("d.web-container", "web");
        
        HostConfig hc = new HostConfig()
                .withPortBindings(
                        new PortBinding(Binding.bindPort(80), ExposedPort.tcp(80)),
                        new PortBinding(Binding.bindPort(443), ExposedPort.tcp(443)))
                .withRestartPolicy(RestartPolicy.alwaysRestart())
                .withLogConfig(new LogConfig(LogConfig.LoggingType.DEFAULT, getLogConfig()));
        
        List<Bind> binds = new ArrayList<>();
        binds.add(new Bind(config.get("d.web"), new Volume("/usr/share/nginx/html"), AccessMode.ro));
        binds.add(new Bind(config.get("d.default.conf"), new Volume("/etc/nginx/conf.d/default.conf"), AccessMode.ro));
        addCertbotBinds(binds);
        hc.withBinds(binds);

        docker.createContainerCmd(image)
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

    private void addCertbotBinds(List<Bind> binds) {
        binds.add(new Bind(config.get("d.certbot") + "/conf", new Volume("/etc/letsencrypt"), AccessMode.rw));
        binds.add(new Bind(config.get("d.certbot") + "/www", new Volume("/var/www/certbot"), AccessMode.rw));
    }
    
    public void startCertbotContainer() {
        String image = config.get("d.certbot-image", "certbot/certbot");
        String name = config.get("d.certbot-container", "certbot");
        
        String mail = config.get("d.mail");
        String domains = new DomainAccess().list().stream().map(i -> " -d " + i.getPublicDomain()).collect(Collectors.joining());
        String cmd = "certonly --email " + mail + " --webroot -w /var/www/certbot --force-renewal --agree-tos --non-interactive" + domains;
        Logger.info(cmd);
        
        HostConfig hc = new HostConfig().withRestartPolicy(RestartPolicy.alwaysRestart());
        List<Bind> binds = new ArrayList<>();
        addCertbotBinds(binds);
        hc.withBinds(binds);

        docker.createContainerCmd(image)
            .withCmd(cmd.split(" "))
            .withName(name)
            .withHostConfig(hc)
            .exec();
    
        docker.startContainerCmd(name).exec();
    }
    
    /**
     * @param phase 0: only http phase, 1: creating certificates phase, 2: https phase
     */
    public void writeDefaultConf(int phase) {
        StringBuilder sb = new StringBuilder();
        List<Domain> domains = new DomainAccess().list();
        write(phase, domains, true, sb);
        write(phase, domains, false, sb);

        FileService.saveTextFile(getDefaultConfFile(), sb.toString());
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
}
