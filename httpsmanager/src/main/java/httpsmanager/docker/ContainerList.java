package httpsmanager.docker;

import java.util.List;

import org.pmw.tinylog.Logger;

import com.github.template72.data.DataList;

import github.soltaufintel.amalia.web.action.Page;
import github.soltaufintel.amalia.web.config.AppConfig;
import httpsmanager.HttpsManagerApp;

public class ContainerList extends Page {

    @Override
    protected void execute() {
        Logger.info("/container");
        
        HttpsManagerApp.initDocker(new AppConfig());
        List<String> containerNames = HttpsManagerApp.docker.getContainerNames();
        
        put("title", "Docker Container");
        DataList list = list("containers");
        for (String name : containerNames) {
            list.add().put("name", esc(name));
        }
    }
}
