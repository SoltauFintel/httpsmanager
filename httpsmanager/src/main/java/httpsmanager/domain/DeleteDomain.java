package httpsmanager.domain;

import org.pmw.tinylog.Logger;

import github.soltaufintel.amalia.web.action.Action;

public class DeleteDomain extends Action {

    @Override
    protected void execute() {
        String id = ctx.pathParam("id");
        
        new DomainAccess().delete(id);
        Logger.info("delete domain: " + id);

        ctx.redirect("/domain");
    }
}
