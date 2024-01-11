package httpsmanager.state;

import github.soltaufintel.amalia.web.action.Action;
import httpsmanager.HttpsManagerApp;

public class StateAction extends Action {

    @Override
    protected void execute() {
    }
    
    @Override
    protected String render() {
        String ret = HttpsManagerApp.stateOk ? "ok" : "not ok";
        if (!HttpsManagerApp.stateOk) {
            ctx.res.status(500);
        }
        return ret;
    }
}
