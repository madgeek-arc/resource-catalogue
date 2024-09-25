package gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates;

import io.swagger.v3.oas.annotations.media.Schema;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ConfigurationTemplateInstance implements Identifiable {

    @XmlElement
    @Schema(example = "(required on PUT only)")
    private String id;

    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, containsResourceId = true)
    private String resourceId;

    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = ConfigurationTemplate.class)
    private String configurationTemplateId;

    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String payload;

    public ConfigurationTemplateInstance() {
    }

    public ConfigurationTemplateInstance(String id, String resourceId, String configurationTemplateId, String payload) {
        this.id = id;
        this.resourceId = resourceId;
        this.configurationTemplateId = configurationTemplateId;
        this.payload = payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigurationTemplateInstance that = (ConfigurationTemplateInstance) o;
        return Objects.equals(id, that.id) && Objects.equals(resourceId, that.resourceId) && Objects.equals(configurationTemplateId, that.configurationTemplateId) && Objects.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, resourceId, configurationTemplateId, payload);
    }

    @Override
    public String toString() {
        return "ConfigurationTemplateInstance{" +
                "id='" + id + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", configurationTemplateId='" + configurationTemplateId + '\'' +
                ", payload='" + payload + '\'' +
                '}';
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getConfigurationTemplateId() {
        return configurationTemplateId;
    }

    public void setConfigurationTemplateId(String configurationTemplateId) {
        this.configurationTemplateId = configurationTemplateId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
