package httpsmanager;

import github.soltaufintel.amalia.mail.MailSender;
import github.soltaufintel.amalia.web.builder.WebAppBuilder;
import github.soltaufintel.amalia.web.route.RouteDefinitions;
import httpsmanager.auth.SimpleAuth;
import httpsmanager.domain.AddDomain;
import httpsmanager.domain.DeleteDomain;
import httpsmanager.domain.DomainList;
import httpsmanager.domain.EditDomain;
import httpsmanager.start.Index;

public class HttpsManagerApp extends RouteDefinitions {
    public static final String VERSION = "0.1.0";
    
    public static void main(String[] args) {
        MailSender.active = false;
        new WebAppBuilder(VERSION)
            .withAuth(new SimpleAuth())
            .withTemplatesFolders(HttpsManagerApp.class, "/templates")
            .withRoutes(new HttpsManagerApp())
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
    }
}
