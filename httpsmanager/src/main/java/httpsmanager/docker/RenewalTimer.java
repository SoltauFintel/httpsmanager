package httpsmanager.docker;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.pmw.tinylog.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

import github.soltaufintel.amalia.mail.Mail;
import github.soltaufintel.amalia.mail.MailSender;
import github.soltaufintel.amalia.timer.BaseTimer;
import github.soltaufintel.amalia.web.config.AppConfig;
import httpsmanager.HttpsManagerApp;

public class RenewalTimer extends BaseTimer {

    @Override
    protected void config() throws SchedulerException {
        start("0 0  9  4 * ?");
    }

    @Override
    protected void timerEvent(JobExecutionContext context) throws Exception {
        String text = "[Renewal timer event] ";
        try {
            Logger.info(text + now());
            String cmd = "renew";
            String log = HttpsManagerApp.docker.runCertbot(cmd);
            text += "`certbot " + cmd + "` response: \n" + log;
            Logger.info(text);
        } catch (Exception e) {
            Logger.error(e);
            text += "error: \n" + e.getMessage() + "\n\nSee console log for details.";
        }
        sendMail(text);
    }

    private void sendMail(String text) {
        AppConfig config = new AppConfig();
        String to = config.get("mail.send-to");
        if (to == null || to.isBlank()) return;
        Mail mail = new Mail();
        mail.setToEmailaddress(to);
        mail.setSendername("https-manager");
        mail.setSubject("Renewal of server certificates");
        mail.setBody(text + "\n<< " + now());
        new MailSender().send(mail, config);
    }

    public static String now() {
        return DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").format(LocalDateTime.now());
    }
}
