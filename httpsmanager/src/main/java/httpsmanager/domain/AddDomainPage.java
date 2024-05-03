package httpsmanager.domain;

import github.soltaufintel.amalia.web.action.Page;
import github.soltaufintel.amalia.web.config.AppConfig;

public class AddDomainPage extends Page {

    @Override
    protected void execute() {
        DomainAccess domainAccess = new DomainAccess();
        if (isPOST()) {
            Domain d = new Domain();
            d.setPublicDomain(ctx.formParam("publicDomain"));
            d.setInternalDomain(ctx.formParam("internalDomain"));
            d.setCertificateName(ctx.formParam("certificateName"));
            d.setRoot("on".equals(ctx.formParam("root")));
            domainAccess.save(d);
            ctx.redirect("/domain");
        } else {
            String id = ctx.queryParam("id");
            Domain d = null;
            if (id != null && !id.isEmpty()) {
                d = domainAccess.get(id);
            }
            put("title", d == null ? "Domain hinzuf&uuml;gen" : "Domain kopieren");
            put("publicDomain", d == null ? "" : esc(d.getPublicDomain()));
            put("internalDomain", d == null ? "" : esc(d.getInternalDomain()));
            put("certificateName", esc(d == null ? new AppConfig().get("default-certificate-name") : d.getCertificateName()));
            if (domainAccess.list().size() == 0) {
                put("internalDomain", "root /usr/share/nginx/html");
            }
        }
    }
}
