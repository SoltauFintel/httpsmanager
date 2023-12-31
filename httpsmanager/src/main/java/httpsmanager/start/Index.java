package httpsmanager.start;

import github.soltaufintel.amalia.auth.Auth;
import github.soltaufintel.amalia.web.action.Page;

public class Index extends Page {

    @Override
    protected void execute() {
        put("login", esc(Auth.auth.getService(ctx).getLogin()));
    }
}
