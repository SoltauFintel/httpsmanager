package httpsmanager.docker;

import org.pmw.tinylog.Logger;

import github.soltaufintel.amalia.web.action.Action;
import httpsmanager.HttpsManagerApp;

public class StopContainers extends Action {

    @Override
    protected void execute() {
        Logger.info("deleting web+certbot containers...");

        HttpsManagerApp.docker.deleteWebContainer();
        HttpsManagerApp.docker.deleteCertbotContainer();
        
        Logger.info("  deleting containers finished");
        
        ctx.redirect("/");
    }
}
