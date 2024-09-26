package gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates;

import io.swagger.v3.oas.annotations.media.Schema;
import org.json.simple.JSONObject;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiable;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecord;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ConfigurationTemplate implements Identifiable {

    @XmlElement
    @Schema(example = "(required on PUT only)")
    @FieldValidation
    private String id;

    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = InteroperabilityRecord.class)
    private String interoperabilityRecordId;

    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String name;

    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String description;

    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
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
