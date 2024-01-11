package httpsmanager.state;

import org.pmw.tinylog.Logger;

import github.soltaufintel.amalia.web.action.Action;
import httpsmanager.HttpsManagerApp;

public class SetStateAction extends Action {

    @Override
    protected void execute() {
        String state = ctx.queryParam("state");
        
        if (state == null || state.isEmpty()) {
            HttpsManagerApp.stateOk = true;
        } else {
            HttpsManagerApp.stateOk = false;
        }
        Logger.info("set state to " + (HttpsManagerApp.stateOk ? "ok" : "not ok"));

        ctx.redirect("/");
    }
}
