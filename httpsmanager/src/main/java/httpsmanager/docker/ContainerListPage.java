package httpsmanager.docker;

import java.util.List;

import com.github.template72.data.DataList;

import github.soltaufintel.amalia.web.action.Page;
import httpsmanager.HttpsManagerApp;

/**
 * Gibt die Namen aller laufenden Docker Container aus.
 * Prüfen, ob überhaupt Zugriff auf Docker möglich ist.
 */
public class ContainerListPage extends Page {

    @Override
    protected void execute() {
        List<String> containerNames = HttpsManagerApp.docker.getContainerNames(false);
        
        put("title", "Docker Container");
        DataList list = list("containers");
        for (String name : containerNames) {
            list.add().put("name", esc(name));
        }
    }
}
