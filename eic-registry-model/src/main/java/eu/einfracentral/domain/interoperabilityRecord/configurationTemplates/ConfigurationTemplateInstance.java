package eu.einfracentral.domain.interoperabilityRecord.configurationTemplates;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.domain.Identifiable;
import io.swagger.annotations.ApiModelProperty;
import org.json.simple.JSONObject;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ConfigurationTemplateInstance implements Identifiable {

    @XmlElement
    @ApiModelProperty(position = 1, example = "(required on PUT only)")
    private String id;

    @XmlElement(required = true)
    @ApiModelProperty(position = 2, required = true)
    @FieldValidation(containsId = true, containsResourceId = true)
    private String resourceId;

    @XmlElement(required = true)
    @ApiModelProperty(position = 3, required = true)
    @FieldValidation(containsId = true, idClass = ConfigurationTemplate.class)
    private String configurationTemplateId;

    @XmlElement(required = true)
    @ApiModelProperty(position = 4, required = true)
    @FieldValidation
    private JSONObject payload;

    public ConfigurationTemplateInstance() {
    }

    public ConfigurationTemplateInstance(String id, String resourceId, String configurationTemplateId, JSONObject payload) {
        this.id = id;
        this.resourceId = resourceId;
        this.configurationTemplateId = configurationTemplateId;
        this.payload = payload;
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

    public JSONObject getPayload() {
        return payload;
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }
}
