package httpsmanager.domain;

public class Domain {
    private String id;
    private String publicDomain;
    private String internalDomain;
    private String certificateName;
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPublicDomain() {
        return publicDomain;
    }

    public void setPublicDomain(String publicDomain) {
        this.publicDomain = publicDomain;
    }

    public String getInternalDomain() {
        return internalDomain;
    }

    public void setInternalDomain(String internalDomain) {
        this.internalDomain = internalDomain;
    }

    public String getCertificateName() {
        return certificateName;
    }

    public void setCertificateName(String certificateName) {
        this.certificateName = certificateName;
    }
}
