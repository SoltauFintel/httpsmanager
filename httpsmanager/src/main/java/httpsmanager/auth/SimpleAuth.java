package httpsmanager.auth;

import github.soltaufintel.amalia.auth.Auth;
import github.soltaufintel.amalia.auth.IUserService;
import github.soltaufintel.amalia.auth.rememberme.IKnownUser;
import github.soltaufintel.amalia.auth.rememberme.NoOpRememberMe;
import github.soltaufintel.amalia.auth.webcontext.WebContext;
import github.soltaufintel.amalia.web.config.AppConfig;
import spark.Spark;

public class SimpleAuth extends Auth {
    private final IUserService userService = new SimpleUserService();

    public SimpleAuth() {
        super(new NoOpRememberMe(), new AppConfig().getInt("encryption-frequency", 0), new SimpleAuthRoutes());
    }

    @Override
    protected IUserService getUserService() {
        return userService;
    }
    
    @Override
    public void filter(WebContext ctx) {
        String path = ctx.path();
        if (isProtected(path) && !ctx.session().isLoggedIn()) {
            IKnownUser knownUser = getRememberMe().getUserIfKnown(ctx);
            if (knownUser != null) {
                ctx.session().setUserId(knownUser.getUserId());
                ctx.session().setLogin(knownUser.getUser());
                ctx.session().setLoggedIn(true);
                return;
            }
            if (!"/login".equals(path)) { // TODO Amalia
                ctx.session().setGoBackPath(path); // Go back to this page after login
            }
            Spark.halt(401, (String) ctx.handle(getRoutes().getLoginPageRouteHandler()));
        }
    }
}
