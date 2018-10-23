package eu.einfracentral.domain;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
@XmlAccessorType(XmlAccessType.FIELD)
public class VocabularyEntry {

    @XmlElement(required = true)
    private String entryId;

    @XmlElement
    private String entryName;

    @XmlElementWrapper(name = "children")
    @XmlElement(name = "child")
    private List<VocabularyEntry> children;

    @XmlJavaTypeAdapter(MapAdapter.class)
    private Map<String, String> extras = new HashMap<>();

    public String getId() {
        return entryId;
    }

    public void setId(String id) {
        this.entryId = id;
    }

    public String getName() {
        return entryName;
    }

    public void setName(String name) {
        this.entryName = name;
    }

    public List<VocabularyEntry> getChildren() {
        return children;
    }

    public void setChildren(List<VocabularyEntry> children) {
        this.children = children;
    }

    public Map<String, String> getExtras() {
        return extras;
    }

    public void setExtras(Map<String, String> extras) {
        this.extras = extras;
    }
}