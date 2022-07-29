package eu.einfracentral.domain;

import com.google.gson.Gson;
import eu.einfracentral.annotation.FieldValidation;

import javax.xml.bind.annotation.XmlElement;

public class ResourceBundle <T extends Service> extends Bundle<T> {

    @XmlElement
    private String status;

    @XmlElement
    @FieldValidation(nullable = true)
    private ResourceExtras resourceExtras;

    public ResourceBundle() {
        // No arg constructor
    }

    public ResourceBundle(T resource) {
        this.setPayload(resource);
        this.setMetadata(null);
    }

    public ResourceBundle(T resource, Metadata metadata) {
        this.setPayload(resource);
        this.setMetadata(metadata);
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ResourceExtras getResourceExtras() {
        return resourceExtras;
    }

    public void setResourceExtras(ResourceExtras resourceExtras) {
        this.resourceExtras = resourceExtras;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceBundle)) return false;
        if (!super.equals(o)) return false;

        ResourceBundle<?> that = (ResourceBundle<?>) o;

        if (getStatus() != null ? !getStatus().equals(that.getStatus()) : that.getStatus() != null) return false;
        return getResourceExtras() != null ? getResourceExtras().equals(that.getResourceExtras()) : that.getResourceExtras() == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getStatus() != null ? getStatus().hashCode() : 0);
        result = 31 * result + (getResourceExtras() != null ? getResourceExtras().hashCode() : 0);
        return result;
    }
}
