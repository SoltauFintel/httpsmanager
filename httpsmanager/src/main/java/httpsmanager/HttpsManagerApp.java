package httpsmanager;

import github.soltaufintel.amalia.web.builder.WebAppBuilder;
import github.soltaufintel.amalia.web.route.RouteDefinitions;
import httpsmanager.start.Index;

public class HttpsManagerApp extends RouteDefinitions {
    public static final String VERSION = "0.1.0";
    
    public static void main(String[] args) {
        new WebAppBuilder(VERSION)
//            .withAuth(new SimpleAuth())
            .withTemplatesFolders(HttpsManagerApp.class, "/templates")
            .withRoutes(new HttpsManagerApp())
            .build()
            .boot();
    }

    @Override
    public void routes() {
        get("/", Index.class);
    }
}
