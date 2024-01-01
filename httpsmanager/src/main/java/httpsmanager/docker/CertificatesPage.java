package httpsmanager.docker;

import github.soltaufintel.amalia.web.action.Page;
import httpsmanager.HttpsManagerApp;

public class CertificatesPage extends Page {

    @Override
    protected void execute() {
        String certificates = HttpsManagerApp.docker.certificates();
        put("certificates", esc(certificates));
        put("title", "certificates");
    }
}
