package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Arrays;
import java.util.Map;

@XmlType
@XmlRootElement
public class Vocabulary implements Identifiable {

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

    public Vocabulary() {
    }

    public Vocabulary(String id, String name, String description, String parentId,
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
        PHASE("Phase"),
        SCIENTIFIC_DOMAIN("Scientific domain"),
        SCIENTIFIC_SUBDOMAIN("Scientific subdomain"),
        TARGET_USER("Target user"),
        ACCESS_TYPE("Access type"),
        ACCESS_MODE("Access mode"),
        PROVIDER_AREA_OF_ACTIVITY("Provider area of activity"),
        PROVIDER_ESFRI_TYPE("Provider esfri type"),
        PROVIDER_ESFRI_DOMAIN("Provider esfri domain"),
        PROVIDER_LEGAL_STATUS("Provider legal status"),
        PROVIDER_LIFE_CYCLE_STATUS("Provider life cycle status"),
        PROVIDER_NETWORK("Provider network"),
        PROVIDER_SOCIETAL_GRAND_CHALLENGE("Provider societal grand challenge"),
        PROVIDER_STRUCTURE_TYPE("Provider structure type"),
        PROVIDER_MERIL_SCIENTIFIC_SUBDOMAIN("Provider meril scientific subdomain");

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
    public String toString() {
        return "Vocabulary{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", parentId='" + parentId + '\'' +
                ", type='" + type + '\'' +
                ", extras=" + extras +
                '}';
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
