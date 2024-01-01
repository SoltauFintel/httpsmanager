package httpsmanager.docker;

import org.pmw.tinylog.Logger;

import com.github.template72.data.DataList;

import github.soltaufintel.amalia.web.action.Page;
import httpsmanager.HttpsManagerApp;

public class DeleteOldCertbotContainers extends Page {
    private int n = 0;
    
    @Override
    protected void execute() {
        put("title", "Delete old certbot containers");
        DataList list = list("containers");
        String image = HttpsManagerApp.docker.certbotImage();
        put("image", esc(image));
        HttpsManagerApp.docker.getContainers(true).forEach(c -> {
            if ("exited".equals(c.getState()) && image.equals(c.getImage())) {
                String container = c.getNames()[0];
                new Thread(() -> {
                    HttpsManagerApp.docker.rmf(container);
                    Logger.info("Container gelöscht: " + container);
                }).start();
                list.add().put("name", container);
                n++;
            }
        });
        put("hasContainers", n > 0);
        putInt("n", n);
    }
}
