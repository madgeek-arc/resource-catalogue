package eu.einfracentral.domain;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;

@XmlRootElement(namespace = "http://einfracentral.eu")
@XmlAccessorType(XmlAccessType.FIELD)
public class Vocabulary implements Identifiable {

    @XmlElement(required = true)
    private String id;

    @XmlElement(required = true)
    private String name;

    @XmlJavaTypeAdapter(EntryMapAdapter.class)
    private Map<String, VocabularyEntry> entries;  // = new HashMap<>();

    public enum Types {
        CATEGORIES("categories"),
        LANGUAGES("languages"),
        PLACES("places"),
        TRL("trl"),
        LIFE_CYCLE_STATUS("lifecyclestatus");

        private final String type;

        Types(final String type) {
            this.type = type;
        }

        public String getKey() {
            return type;
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, VocabularyEntry> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, VocabularyEntry> entries) {
        this.entries = entries;
    }
}