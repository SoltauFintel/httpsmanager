package httpsmanager.docker;

import github.soltaufintel.amalia.web.action.Action;

public class StartContainersAction extends Action {

    @Override
    protected void execute() {
        int phase = Integer.parseInt(ctx.pathParam("phase"));
        
        StartPhase.start(phase);
        
        ctx.redirect("/");
    }
}
