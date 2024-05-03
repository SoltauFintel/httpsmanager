package httpsmanager.docker;

import org.pmw.tinylog.Logger;

import github.soltaufintel.amalia.web.action.Action;
import httpsmanager.HttpsManagerApp;

public class StopContainersAction extends Action {

    @Override
    protected void execute() {
        new Thread(() -> {
            HttpsManagerApp.docker.deleteWebContainer();
            Logger.info("deleting web container finished");
            HttpsManagerApp.docker.deleteCertbotContainer();
            Logger.info("deleting certbot container finished");
        }).start();
        Logger.info("web+certbot Container loeschen Anfrage angenommen");
        
        ctx.redirect("/");
    }
}
