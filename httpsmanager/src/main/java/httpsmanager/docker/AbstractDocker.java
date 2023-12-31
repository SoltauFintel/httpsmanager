package httpsmanager.docker;

import java.util.List;

import org.pmw.tinylog.Logger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;

public abstract class AbstractDocker {
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
}
