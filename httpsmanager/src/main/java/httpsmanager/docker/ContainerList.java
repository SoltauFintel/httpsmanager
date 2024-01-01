package httpsmanager.docker;

import java.util.List;

import com.github.template72.data.DataList;

import github.soltaufintel.amalia.web.action.Page;
import httpsmanager.HttpsManagerApp;

/**
 * Gibt die Namen aller laufenden Docker Container aus.
 * Prüfen, ob überhaupt Zugriff auf Docker möglich ist.
 */
public class ContainerList extends Page {

    @Override
    protected void execute() {
        List<String> containerNames = HttpsManagerApp.docker.getContainerNames();
        put("title", "Docker Container");
        DataList list = list("containers");
        for (String name : containerNames) {
            list.add().put("name", esc(name));
        }

        String certificates = ""; //HttpsManagerApp.docker.certificates();   Hierfür eine Extrafunktion machen
        put("certificates", esc(certificates));
        
        // TODO Die Buttons auf die Startseite bringen. Hier nur Containerliste ausgeben.
    }
}
