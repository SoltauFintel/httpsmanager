package httpsmanager.docker;

import org.pmw.tinylog.Logger;

import github.soltaufintel.amalia.web.action.Action;
import httpsmanager.HttpsManagerApp;

public class RenewalAction extends Action {

    @Override
    protected void execute() {
        new Thread(() -> {
            HttpsManagerApp.docker.deleteCertbotContainer();
            HttpsManagerApp.docker.startCertbotContainer();
            Logger.info("Renewal: certbot Container wurde gestartet.");
        }).start();
        Logger.info("Renewal Anfrage angenommen");
        ctx.redirect("/");
    }
}
