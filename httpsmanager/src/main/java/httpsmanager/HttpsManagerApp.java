package httpsmanager;

import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;

import github.soltaufintel.amalia.web.action.Action;
import github.soltaufintel.amalia.web.builder.LoggingInitializer;
import github.soltaufintel.amalia.web.builder.WebAppBuilder;
import github.soltaufintel.amalia.web.config.AppConfig;
import github.soltaufintel.amalia.web.route.RouteDefinitions;
import httpsmanager.auth.SimpleAuth;
import httpsmanager.docker.AbstractDocker;
import httpsmanager.docker.CertificatesPage;
import httpsmanager.docker.CheckCertificatesPage;
import httpsmanager.docker.ContainerListPage;
import httpsmanager.docker.RenewalPage;
import httpsmanager.docker.StartContainersAction;
import httpsmanager.docker.StopContainersAction;
import httpsmanager.docker.UnixDocker;
import httpsmanager.docker.WindowsDocker;
import httpsmanager.docker.timer.CheckCertificatesTimer;
import httpsmanager.docker.timer.RenewalTimer;
import httpsmanager.domain.AddDomainPage;
import httpsmanager.domain.DeleteDomainAction;
import httpsmanager.domain.DomainListPage;
import httpsmanager.domain.EditDomainPage;
import httpsmanager.domain.InitDomains;
import httpsmanager.start.IndexPage;
import httpsmanager.state.SetStateAction;
import httpsmanager.state.StateAction;

public class HttpsManagerApp extends RouteDefinitions {
    public static final String VERSION = "1.1.0";
    public static AbstractDocker docker;
    public static boolean stateOk = false;
    
    public static void main(String[] args) {
        new WebAppBuilder(VERSION)
        	.withLogging(new LoggingInitializer(Level.INFO, "{date} {level}  {message}"))
            .withAuth(new SimpleAuth())
            .withTemplatesFolders(HttpsManagerApp.class, "/templates")
            .withInitializer(config -> initDocker(config))
            .withInitializer(config -> new CheckCertificatesTimer().start())
            .withInitializer(config -> new RenewalTimer().start())
            .withRoutes(new HttpsManagerApp())
            .withRoutes(new MyPingRouteDefinition())
            .build()
            .boot();
        stateOk = true;
    }

    @Override
    public void routes() {
        get("/", IndexPage.class);
        
        // Domains
        form("/domain/:id/edit", EditDomainPage.class);
        get("/domain/:id/delete", DeleteDomainAction.class);
        form("/domain/add", AddDomainPage.class);
        get("/domain", DomainListPage.class);
        
        // Docker
        get("/container", ContainerListPage.class);
        get("/start/:phase", StartContainersAction.class);
        get("/stop", StopContainersAction.class);
        get("/certificates", CertificatesPage.class);
        get("/check-certificates", CheckCertificatesPage.class);
        get("/renewal", RenewalPage.class);
        
        // State for monitoring
        get("/rest/state", StateAction.class);
        addNotProtected("/rest/state");
        get("/state", SetStateAction.class);
        
        // TODO /rest/_info
    }

    public static class MyPingRouteDefinition extends RouteDefinitions {

        public MyPingRouteDefinition() {
            super(29);
        }
        
        @Override
        public void routes() {
            get("/rest/_ping", PingAction.class);
        }
        
        public static class PingAction extends Action {

            @Override
            protected void execute() {
                System.out.println("ping neu");
            }
            
            @Override
            protected String render() {
                return "pong"; // TODO Amalia
            }
        }
    }
    
    public static void initDocker(AppConfig config) {
        if (config.isDevelopment()) {
            docker = new WindowsDocker();
        } else {
            docker = new UnixDocker();
        }
        Logger.debug(docker.getClass().getSimpleName());
        
        InitDomains.createDomainsFromConfig(config);
    }
}
