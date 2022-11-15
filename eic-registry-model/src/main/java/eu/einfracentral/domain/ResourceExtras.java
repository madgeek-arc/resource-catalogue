package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.VocabularyValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ResourceExtras {

    @XmlElementWrapper(name = "eoscIFGuidelines")
    @XmlElement(name = "eoscIFGuideline")
    @ApiModelProperty(position = 1)
    @FieldValidation(nullable = true)
    private List<EOSCIFGuidelines> eoscIFGuidelines;

    @XmlElement()
    @ApiModelProperty(position = 2)
    private boolean horizontalService;

    @XmlElementWrapper(name = "researchCategories")
    @XmlElement(name = "researchCategory")
    @ApiModelProperty(position = 3)
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.RESEARCH_CATEGORY)
    private List<String> researchCategories;

    /**
     * List of Interoperability Record IDs
     */
    @XmlElementWrapper(name = "interoperabilityRecordIds")
    @XmlElement(name = "interoperabilityRecordId")
    @ApiModelProperty(position = 4)
    @FieldValidation(nullable = true, containsId = true, idClass = InteroperabilityRecord.class)
    private List<String> interoperabilityRecordIds;

    public ResourceExtras() {
    }

    public ResourceExtras(List<EOSCIFGuidelines> eoscIFGuidelines, boolean horizontalService, List<String> researchCategories, List<String> interoperabilityRecordIds) {
        this.eoscIFGuidelines = eoscIFGuidelines;
        this.horizontalService = horizontalService;
        this.researchCategories = researchCategories;
        this.interoperabilityRecordIds = interoperabilityRecordIds;
    }

    @Override
    public String toString() {
        return "ResourceExtras{" +
                "eoscIFGuidelines=" + eoscIFGuidelines +
                ", horizontalService=" + horizontalService +
                ", researchCategories=" + researchCategories +
                ", interoperabilityRecordIds=" + interoperabilityRecordIds +
                '}';
    }

    public List<EOSCIFGuidelines> getEoscIFGuidelines() {
        return eoscIFGuidelines;
    }

    public void setEoscIFGuidelines(List<EOSCIFGuidelines> eoscIFGuidelines) {
        this.eoscIFGuidelines = eoscIFGuidelines;
    }

    public boolean isHorizontalService() {
        return horizontalService;
    }

    public void setHorizontalService(boolean horizontalService) {
        this.horizontalService = horizontalService;
    }

    public List<String> getResearchCategories() {
        return researchCategories;
    }

    public void setResearchCategories(List<String> researchCategories) {
        this.researchCategories = researchCategories;
    }

    public List<String> getInteroperabilityRecordIds() {
        return interoperabilityRecordIds;
    }

    public void setInteroperabilityRecordIds(List<String> interoperabilityRecordIds) {
        this.interoperabilityRecordIds = interoperabilityRecordIds;
    }
}
