package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.VocabularyValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class InfraServiceExtras {

    @XmlElement()
    private List<EOSCIFGuidelines> eoscIFGuidelines;

    @XmlElement()
    private boolean horizontalService;

    @XmlElementWrapper(name = "researchCategories")
    @XmlElement(name = "researchCategory")
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.RESEARCH_CATEGORY)
    private List<String> researchCategories;

    public InfraServiceExtras() {
    }

    public InfraServiceExtras(List<EOSCIFGuidelines> eoscIFGuidelines, boolean horizontalService, List<String> researchCategories) {
        this.eoscIFGuidelines = eoscIFGuidelines;
        this.horizontalService = horizontalService;
        this.researchCategories = researchCategories;
    }

    @Override
    public String toString() {
        return "InfraServiceExtras{" +
                "eoscIFGuidelines=" + eoscIFGuidelines +
                ", horizontalService=" + horizontalService +
                ", researchCategories=" + researchCategories +
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
}
