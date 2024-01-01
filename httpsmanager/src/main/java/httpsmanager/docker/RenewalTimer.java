package httpsmanager.docker;

import java.time.LocalDateTime;

import org.pmw.tinylog.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

import github.soltaufintel.amalia.timer.BaseTimer;
import httpsmanager.HttpsManagerApp;

public class RenewalTimer extends BaseTimer {

    @Override
    protected void config() throws SchedulerException {
        start("0 0  9  4 * ?");
    }

    @Override
    protected void timerEvent(JobExecutionContext context) throws Exception {
        Logger.info("Renewal event! " + LocalDateTime.now().toString());
        String cmd = "renew";
        String log = HttpsManagerApp.docker.runCertbot(cmd);
        String text = "[Renewal timer event] `certbot " + cmd + "` response: \n" + log + "\n<< " + LocalDateTime.now().toString();
        Logger.info(text);
    }
}
