package httpsmanager.domain;

import github.soltaufintel.amalia.web.action.Page;

public class EditDomain extends Page {

    @Override
    protected void execute() {
        String id = ctx.pathParam("id");
        Domain d = new DomainAccess().get(id);
        if (d == null) {
            throw new RuntimeException("Domain nicht vorhanden");
        }
        if (isPOST()) {
            d.setPublicDomain(ctx.formParam("publicDomain"));
            d.setInternalDomain(ctx.formParam("internalDomain"));
            d.setCertificateName(ctx.formParam("certificateName"));
            d.setRoot("on".equals(ctx.formParam("root")));
            new DomainAccess().save(d);
            ctx.redirect("/domain");
        } else {
            put("title", "Domain bearbeiten");
            put("id", esc(d.getId()));
            put("publicDomain", esc(d.getPublicDomain()));
            put("internalDomain", esc(d.getInternalDomain()));
            put("certificateName", esc(d.getCertificateName()));
            put("root", d.isRoot());
        }
    }
}
