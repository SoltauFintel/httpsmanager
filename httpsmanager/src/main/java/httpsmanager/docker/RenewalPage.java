package httpsmanager.docker;

import org.pmw.tinylog.Logger;

import github.soltaufintel.amalia.web.action.Page;
import httpsmanager.HttpsManagerApp;

public class RenewalPage extends Page {

    @Override
    protected void execute() {
        boolean dryRun = "1".equals(ctx.queryParam("dry-run"));
        String cmd = dryRun ? "renew --dry-run" : "renew";
        String log = HttpsManagerApp.docker.runCertbot(cmd);
        String text = "`certbot " + cmd + "` response: \n" + log;
        put("response", text);
        put("title", cmd);
        Logger.info(text);
    }
}
