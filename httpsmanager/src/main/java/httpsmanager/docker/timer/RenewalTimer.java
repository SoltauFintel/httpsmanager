package httpsmanager.docker.timer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

import org.pmw.tinylog.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

import github.soltaufintel.amalia.mail.Mail;
import github.soltaufintel.amalia.mail.MailSender;
import github.soltaufintel.amalia.timer.BaseTimer;
import github.soltaufintel.amalia.web.config.AppConfig;
import httpsmanager.HttpsManagerApp;

public class RenewalTimer extends BaseTimer {
    public static int checkday = -1;
    
    @Override
    protected void config() throws SchedulerException {
        start(new AppConfig().get("RenewalTimer.cron", "0 0 9 * * ?")); // every day 09:00
    }

    @Override
    protected void timerEvent(JobExecutionContext context) throws Exception {
        int day = LocalDate.now().getDayOfMonth();
        if (day != 4 && day != 18 && day != checkday) {
            return;
        }
        
        String text = "[Renewal timer event] ";
        try {
            Logger.info(text + now());
            String cmd = "renew";
            String log = HttpsManagerApp.docker.runCertbot(cmd);
            text += "`certbot " + cmd + "` response: \n" + log;
            Logger.info(text);
            if (text.contains("not yet due for renewal")) {
                HttpsManagerApp.stateOk = true;
            }
        } catch (Exception e) {
            HttpsManagerApp.stateOk = false;
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
        mail.setSendername(config.get("mail.sendername", "https-manager"));
        mail.setSubject(config.get("mail.subject", "Renewal of server certificates"));
        mail.setBody(text + "\n<< " + now());
        new MailSender().send(mail, config);
    }

    public static String now() {
        return DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").format(LocalDateTime.now());
    }

    public static void registerDay(java.util.Date notAfter) {
        Calendar c = Calendar.getInstance();
        c.setTime(notAfter);
        int old = checkday;
        checkday = c.get(Calendar.DAY_OF_MONTH) + 1;
        if (checkday > 28) {
            checkday = 1;
        }
        if (old != checkday) {
            Logger.info("Set checkday to " + checkday);
        }
    }
}
