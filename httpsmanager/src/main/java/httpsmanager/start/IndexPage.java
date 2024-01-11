package httpsmanager.start;

import github.soltaufintel.amalia.auth.Auth;
import github.soltaufintel.amalia.web.action.Page;
import httpsmanager.HttpsManagerApp;

public class IndexPage extends Page {

    @Override
    protected void execute() {
        put("title", "https manager");
        put("login", esc(Auth.auth.getService(ctx).getLogin()));
        put("state", HttpsManagerApp.stateOk ? "ok" : "not ok");
    }
}
