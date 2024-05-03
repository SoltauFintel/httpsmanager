package httpsmanager.docker;

import org.pmw.tinylog.Logger;

import httpsmanager.HttpsManagerApp;

public class StartPhase {

	private StartPhase() {
	}
	
	public static void start(int phase) {
        Logger.info("StartPhase: starting phase " + phase + "...");
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

        Logger.debug("StartPhase(" + phase + ") finished <----");

	}
}
