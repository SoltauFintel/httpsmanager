package httpsmanager.domain;

import github.soltaufintel.amalia.web.action.Page;
import github.soltaufintel.amalia.web.config.AppConfig;

public class AddDomainPage extends Page {

    @Override
    protected void execute() {
        if (isPOST()) {
            Domain d = new Domain();
            d.setPublicDomain(ctx.formParam("publicDomain"));
            d.setInternalDomain(ctx.formParam("internalDomain"));
            d.setCertificateName(ctx.formParam("certificateName"));
            d.setRoot("on".equals(ctx.formParam("root")));
            new DomainAccess().save(d);
            ctx.redirect("/domain");
        } else {
            put("title", "Domain hinzufügen");
            put("certificateName", new AppConfig().get("default-certificate-name"));
        }
    }
}
