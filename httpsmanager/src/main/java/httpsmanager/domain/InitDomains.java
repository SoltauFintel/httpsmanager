package httpsmanager.domain;

import java.util.List;

import org.pmw.tinylog.Logger;

import github.soltaufintel.amalia.web.config.AppConfig;
import httpsmanager.docker.StartPhase;

public class InitDomains {

	private InitDomains() {
	}
	
	public static void createDomainsFromConfig(AppConfig config) {
		boolean added = false;
		DomainAccess access = new DomainAccess();
		List<Domain> domains = access.list();
		int n = config.getInt("domains", 0);
		Logger.info("domains in config: " + n);
		for (int i = 1; i <= n; i++) {
			String pd = config.get("domain." + i + ".public");
			if (pd == null || pd.isBlank()) {
				Logger.debug("domain." + i + ".public is empty -> skip");
				continue;
			}

			boolean found = false;
			for (Domain d : domains) {
				if (d.getPublicDomain().equals(pd)) {
					found = true;
					break;
				}
			}
			if (found) {
				Logger.info("domain already exists: " + pd);
			} else {
				String id = config.get("domain." + i + ".internal");
				String cn = config.get("domain." + i + ".certificate-name");
				boolean root = "true".equals(config.get("domain." + i + ".root"));

				Domain newDomain = new Domain();
				newDomain.setPublicDomain(pd);
				newDomain.setInternalDomain(id);
				newDomain.setCertificateName(cn);
				newDomain.setRoot(root);
				access.save(newDomain);
				domains.add(newDomain);
				added = true;
				Logger.info("Domain added [from config]: " + pd + " | " + id + " | " + cn + " | " + root);
			}
		}
		if (added) {
			Logger.info("InitDomains starting phase 1 and then phase 2 ...");
			StartPhase.start(1);
			StartPhase.start(2);
		}
	}
}
