package eu.einfracentral.domain.interoperabilityRecord.configurationTemplates;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.domain.Identifiable;
import eu.einfracentral.domain.InteroperabilityRecord;
import io.swagger.annotations.ApiModelProperty;
import org.json.simple.JSONObject;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ConfigurationTemplate implements Identifiable {

    @XmlElement
    @ApiModelProperty(position = 1, example = "(required on PUT only)")
    @FieldValidation
    private String id;

    @XmlElement(required = true)
    @ApiModelProperty(position = 2, required = true)
    @FieldValidation(containsId = true, idClass = InteroperabilityRecord.class)
    private String interoperabilityRecordId;

    @XmlElement(required = true)
    @ApiModelProperty(position = 3, required = true)
    @FieldValidation
    private String name;

    @XmlElement(required = true)
    @ApiModelProperty(position = 4, required = true)
    @FieldValidation
    private String description;

    @XmlElement(required = true)
    @ApiModelProperty(position = 4, required = true)
    @FieldValidation
    private JSONObject formModel;

    public ConfigurationTemplate() {
    }

    public ConfigurationTemplate(String id, String interoperabilityRecordId, String name, String description,
                                 JSONObject formModel) {
        this.id = id;
        this.interoperabilityRecordId = interoperabilityRecordId;
        this.name = name;
        this.description = description;
        this.formModel = formModel;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getInteroperabilityRecordId() {
        return interoperabilityRecordId;
    }

    public void setInteroperabilityRecordId(String interoperabilityRecordId) {
        this.interoperabilityRecordId = interoperabilityRecordId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JSONObject getFormModel() {
        return formModel;
    }

    public void setFormModel(JSONObject formModel) {
        this.formModel = formModel;
    }
}
