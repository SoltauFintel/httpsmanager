package httpsmanager.docker;

import org.pmw.tinylog.Logger;

import github.soltaufintel.amalia.web.action.Action;
import httpsmanager.HttpsManagerApp;

public class StartContainers extends Action {

    @Override
    protected void execute() {
        int phase = Integer.parseInt(ctx.pathParam("phase"));

        Logger.info("starting phase " + phase + "...");
        HttpsManagerApp.docker.deleteWebContainer();
        HttpsManagerApp.docker.deleteCertbotContainer();
        
        HttpsManagerApp.docker.writeDefaultConf(phase);
        
        HttpsManagerApp.docker.startWebContainer();
        Logger.info("web container started");

        if (phase > 0) {
            HttpsManagerApp.docker.startCertbotContainer();
            Logger.info("certbot container started");
        } else {
            Logger.info("certbot container not needed");
        }

        ctx.redirect("/");
    }
}
