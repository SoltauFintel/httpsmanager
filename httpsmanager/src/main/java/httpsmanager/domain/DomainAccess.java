package httpsmanager.domain;

import static httpsmanager.base.FileService.loadTextFile;
import static httpsmanager.base.FileService.saveTextFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import github.soltaufintel.amalia.base.IdGenerator;
import github.soltaufintel.amalia.web.config.AppConfig;

public class DomainAccess {

    public List<Domain> list() {
        List<Domain> ret = new ArrayList<>();
        File domainsDir = file("dummy").getParentFile();
        if (domainsDir.isDirectory()) {
            File[] files = domainsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.getName().startsWith(".") && file.getName().endsWith(".json")) {
                        ret.add(new Gson().fromJson(loadTextFile(file), Domain.class));
                    }
                }
            }
        }
        ret.sort((a, b) -> a.sort().compareTo(b.sort()));
        return ret;
    }

    public Domain get(String id) {
        return list().stream().filter(i -> i.getId().equals(id)).findFirst().orElse(null);
    }

    public void save(Domain d) {
        if (d.getPublicDomain() == null || d.getPublicDomain().isBlank()) {
            throw new IllegalArgumentException("publicDomain must not be empty");
        }
        if (d.getId() == null) {
            d.setId(IdGenerator.createId6());
        }
        File file = file(d.getId());
        saveTextFile(file, new Gson().toJson(d));
    }

    public void delete(String id) {
        File file = file(id);
        file.delete();
    }
    
    private File file(String id) {
        if (id == null || id.isBlank() || id.contains("..")) {
            throw new IllegalArgumentException("Illegal id");
        }
        return new File(new AppConfig().get("save-folder"), "domains/" + id + ".json");
    }
}
