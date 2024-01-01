package httpsmanager;

import github.soltaufintel.amalia.mail.MailSender;
import github.soltaufintel.amalia.web.action.Action;
import github.soltaufintel.amalia.web.builder.WebAppBuilder;
import github.soltaufintel.amalia.web.config.AppConfig;
import github.soltaufintel.amalia.web.route.RouteDefinitions;
import httpsmanager.auth.SimpleAuth;
import httpsmanager.docker.AbstractDocker;
import httpsmanager.docker.CertificatesPage;
import httpsmanager.docker.CheckCertificatesPage;
import httpsmanager.docker.ContainerListPage;
import httpsmanager.docker.RenewalAction;
import httpsmanager.docker.StartContainersAction;
import httpsmanager.docker.StopContainersAction;
import httpsmanager.docker.UnixDocker;
import httpsmanager.docker.WindowsDocker;
import httpsmanager.domain.AddDomain;
import httpsmanager.domain.DeleteDomain;
import httpsmanager.domain.DomainList;
import httpsmanager.domain.EditDomain;
import httpsmanager.start.Index;

public class HttpsManagerApp extends RouteDefinitions {
    public static final String VERSION = "0.1.0";
    public static AbstractDocker docker;
    
    public static void main(String[] args) {
        MailSender.active = false;
        new WebAppBuilder(VERSION)
            .withAuth(new SimpleAuth())
            .withTemplatesFolders(HttpsManagerApp.class, "/templates")
            .withInitializer(config -> initDocker(config))
            .withRoutes(new HttpsManagerApp())
            .withRoutes(new MyPingRouteDefinition())
            .build()
            .boot();
    }

    @Override
    public void routes() {
        get("/", Index.class);
        
        // Domains
        form("/domain/:id/edit", EditDomain.class);
        get("/domain/:id/delete", DeleteDomain.class);
        form("/domain/add", AddDomain.class);
        get("/domain", DomainList.class);
        
        // Docker
        get("/container", ContainerListPage.class);
        get("/start/:phase", StartContainersAction.class);
        get("/stop", StopContainersAction.class);
        get("/certificates", CertificatesPage.class);
        get("/check-certificates", CheckCertificatesPage.class);
        get("/renewal", RenewalAction.class);
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
    }
}
