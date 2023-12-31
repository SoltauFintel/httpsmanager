package httpsmanager.auth;

import github.soltaufintel.amalia.auth.Auth;
import github.soltaufintel.amalia.auth.IUserService;
import github.soltaufintel.amalia.auth.rememberme.NoOpRememberMe;
import github.soltaufintel.amalia.web.config.AppConfig;

public class SimpleAuth extends Auth {
    private final IUserService userService = new SimpleUserService();

    public SimpleAuth() {
        super(new NoOpRememberMe(), new AppConfig().getInt("encryption-frequency", 0), new SimpleAuthRoutes());
    }

    @Override
    protected IUserService getUserService() {
        return userService;
    }
}
