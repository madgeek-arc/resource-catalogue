package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URL;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class EOSCIFGuidelines {

    @XmlElement()
    private String pid;

    @XmlElement()
    private String label;

    @XmlElement()
    @FieldValidation(nullable = true)
    private URL url;

    @XmlElement()
    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.SEMANTIC_RELATIONSHIP)
    private String semanticRelationship;

    public EOSCIFGuidelines() {
    }

    public EOSCIFGuidelines(String pid, String label, URL url, String semanticRelationship) {
        this.pid = pid;
        this.label = label;
        this.url = url;
        this.semanticRelationship = semanticRelationship;
    }

    @Override
    public String toString() {
        return "EOSCIFGuidelines{" +
                "pid='" + pid + '\'' +
                ", label='" + label + '\'' +
                ", url=" + url +
                ", semanticRelationship='" + semanticRelationship + '\'' +
                '}';
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getSemanticRelationship() {
        return semanticRelationship;
    }

    public void setSemanticRelationship(String semanticRelationship) {
        this.semanticRelationship = semanticRelationship;
    }
}
