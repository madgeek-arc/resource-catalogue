package eu.einfracentral.domain;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Arrays;
import java.util.Map;

@XmlType
@XmlRootElement
public class NewVocabulary implements Identifiable {

    @XmlElement(required = true)
    private String id;

    @XmlElement(required = true)
    private String name;

    @XmlElement
    private String description;

    @XmlElement
    private String parentId;

    @XmlElement(required = true)
    private String type;

    @XmlJavaTypeAdapter(ExtrasMapTypeAdapter.class)
    private Map<String, String> extras;

    public NewVocabulary() {

    }

    public NewVocabulary(String id, String name, String description, String parentId,
                         String type, Map<String, String> extras) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.parentId = parentId;
        this.type = type;
        this.extras = extras;
    }

    public enum Type {
        SUPERCATEGORY("Supercategory"),
        CATEGORY("Category"),
        SUBCATEGORY("Subcategory"),
        LANGUAGE("Language"),
        PLACE("Place"),
        TRL("Technology readiness level"),
        PHASE("Phase");

        private final String type;

        Type(final String type) {
            this.type = type;
        }

        public String getKey() {
            return type;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static Type fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(Type.values())
                    .filter(v -> v.type.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }
    }

    @Override
    public String getId() {
        return this.id;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type.getKey();
    }

    public Map<String, String> getExtras() {
        return extras;
    }

    public void setExtras(Map<String, String> extras) {
        this.extras = extras;
    }
}
