package httpsmanager.domain;

import github.soltaufintel.amalia.web.action.Page;
import github.soltaufintel.amalia.web.config.AppConfig;

public class AddDomain extends Page {

    @Override
    protected void execute() {
        if (isPOST()) {
            Domain d = new Domain();
            d.setPublicDomain(ctx.formParam("publicDomain"));
            d.setInternalDomain(ctx.formParam("internalDomain"));
            d.setCertificateName(ctx.formParam("certificateName"));
            new DomainAccess().save(d);
            ctx.redirect("/domain");
        } else {
            put("title", "Domain hinzufügen");
            put("certificateName", new AppConfig().get("default-certificate-name"));
        }
    }
}
