package httpsmanager.docker;

import com.github.template72.data.DataList;
import com.github.template72.data.DataMap;

import github.soltaufintel.amalia.web.action.Page;
import httpsmanager.domain.DomainAccess;

public class CheckCertificatesPage extends Page {

    @Override
    protected void execute() {
        DataList list = list("domains");
        put("title", "SSL Zertifikate aller Domains prüfen");
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
            DataMap map = list.add();
            map.put("name", esc(d.getPublicDomain()));
            map.put("state", esc(state));
            map.put("ok", ok);
        });
    }

}
