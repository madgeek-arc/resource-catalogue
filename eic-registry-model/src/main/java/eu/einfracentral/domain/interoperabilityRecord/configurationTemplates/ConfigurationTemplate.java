package eu.einfracentral.domain.interoperabilityRecord.configurationTemplates;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.VocabularyValidation;
import eu.einfracentral.domain.Identifiable;
import eu.einfracentral.domain.InteroperabilityRecord;
import eu.einfracentral.domain.Vocabulary;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;
import java.util.List;

@XmlType
@XmlRootElement
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
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.CT_PROTOCOL)
    private String protocol;

    @XmlElement
    @ApiModelProperty(position = 4)
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.CT_COMPATIBILITY)
    private String compatibility;

    @XmlElement(required = true)
    @ApiModelProperty(position = 5, example = "https://example.com", required = true)
    @FieldValidation
    private URL baseURL;

    @XmlElement(required = true)
    @ApiModelProperty(position = 6, required = true)
    @FieldValidation
    private String format;

    @XmlElementWrapper(name = "sets")
    @XmlElement(name = "set")
    @ApiModelProperty(position = 7)
    @FieldValidation(nullable = true)
    private List<String> sets;

    public ConfigurationTemplate() {
    }

    public ConfigurationTemplate(String id, String interoperabilityRecordId, String protocol, String compatibility, URL baseURL, String format, List<String> sets) {
        this.id = id;
        this.interoperabilityRecordId = interoperabilityRecordId;
        this.protocol = protocol;
        this.compatibility = compatibility;
        this.baseURL = baseURL;
        this.format = format;
        this.sets = sets;
    }

    @Override
    public String toString() {
        return "ConfigurationTemplate{" +
                "id='" + id + '\'' +
                ", interoperabilityRecordId='" + interoperabilityRecordId + '\'' +
                ", protocol='" + protocol + '\'' +
                ", compatibility='" + compatibility + '\'' +
                ", baseURL=" + baseURL +
                ", format='" + format + '\'' +
                ", sets=" + sets +
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

    public String getInteroperabilityRecordId() {
        return interoperabilityRecordId;
    }

    public void setInteroperabilityRecordId(String interoperabilityRecordId) {
        this.interoperabilityRecordId = interoperabilityRecordId;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getCompatibility() {
        return compatibility;
    }

    public void setCompatibility(String compatibility) {
        this.compatibility = compatibility;
    }

    public URL getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(URL baseURL) {
        this.baseURL = baseURL;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public List<String> getSets() {
        return sets;
    }

    public void setSets(List<String> sets) {
        this.sets = sets;
    }
}
