package httpsmanager.docker.timer;

import org.pmw.tinylog.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

import github.soltaufintel.amalia.mail.Mail;
import github.soltaufintel.amalia.mail.MailSender;
import github.soltaufintel.amalia.timer.BaseTimer;
import github.soltaufintel.amalia.web.config.AppConfig;
import httpsmanager.HttpsManagerApp;
import httpsmanager.docker.CertificateService;
import httpsmanager.domain.DomainAccess;

// TODO auf AbstractTimer umstellen
public class CheckCertificatesTimer extends BaseTimer {
    private String msg;
    
    @Override
    protected void config() throws SchedulerException {
        start(new AppConfig().get("CheckCertificatesTimer.cron", "0 50 8 ? * TUE")); // every Tuesday 8:50
    }

    @Override
    protected void timerEvent(JobExecutionContext context) throws Exception {
        Logger.info("CheckCertificatesTimer");
        msg = "";
        // TODO duplicate code
        CertificateService cer = new CertificateService();
        new DomainAccess().list().forEach(d -> {
            String state = "?";
            boolean ok = false;
            try {
                state = cer.checkHttpsUrl("https://" + d.getPublicDomain(), true, true);
                ok = state != null && state.startsWith("ok");
            } catch (Exception e) {
                state = e.getMessage();
            }
            if (state.contains("401") || state.contains("500")) {
                try {
                    state = cer.checkHttpsUrl("https://" + d.getPublicDomain() + "/rest/_ping", true, true) + " (/rest/_ping)";
                    ok = true;
                } catch (Exception e) {
                    state = e.getMessage();
                }
            }
            if (!ok) {
                msg += "\ncheck not ok for: " + d.getPublicDomain() + ": " + state;
            }
        });
        if (msg.isEmpty()) {
            Logger.info("Server certificates are ok.");
            HttpsManagerApp.stateOk = true;
        } else {
            HttpsManagerApp.stateOk = false;
            String text = "There are certificate errors:\n" + msg;
            Logger.info(text);
            sendMail(text);
        }
    }
    
    // TODO duplicate similar method
    private void sendMail(String text) {
        AppConfig config = new AppConfig();
        String to = config.get("mail.send-to");
        if (to == null || to.isBlank()) return;
        Mail mail = new Mail();
        mail.setToEmailaddress(to);
        mail.setSendername("https-manager"); // TODO parametrisieren
        mail.setSubject("Checking server certificates"); // TODO parametrisieren
        mail.setBody(text + "\n<< " + RenewalTimer. now());
        new MailSender().send(mail, config);
    }
}
