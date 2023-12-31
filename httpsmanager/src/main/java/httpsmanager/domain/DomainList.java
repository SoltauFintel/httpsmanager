package httpsmanager.domain;

import java.util.List;

import com.github.template72.data.DataList;
import com.github.template72.data.DataMap;

import github.soltaufintel.amalia.web.action.Page;

public class DomainList extends Page {

    @Override
    protected void execute() {
        List<Domain> domains = new DomainAccess().list();

        DataList list = list("domains");
        for (Domain d : domains) {
            DataMap map = list.add();
            map.put("id", esc(d.getId()));
            map.put("publicDomain", esc(d.getPublicDomain()));
            map.put("internalDomain", esc(d.getInternalDomain()));
            map.put("certificateName", esc(d.getCertificateName()));
            map.put("deletelink", esc("/domain/" + d.getId() + "/delete"));
        }
        putInt("n", domains.size());
    }
}
