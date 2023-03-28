package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

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
        // Provider
        PROVIDER_AREA_OF_ACTIVITY("Provider area of activity"),
        PROVIDER_ESFRI_TYPE("Provider esfri type"),
        PROVIDER_ESFRI_DOMAIN("Provider esfri domain"),
        PROVIDER_HOSTING_LEGAL_ENTITY("Provider hosting legal entity"),
        PROVIDER_LEGAL_STATUS("Provider legal status"),
        PROVIDER_LIFE_CYCLE_STATUS("Provider life cycle status"),
        PROVIDER_NETWORK("Provider network"),
        PROVIDER_SOCIETAL_GRAND_CHALLENGE("Provider societal grand challenge"),
        PROVIDER_STRUCTURE_TYPE("Provider structure type"),
        PROVIDER_MERIL_SCIENTIFIC_DOMAIN("Provider meril scientific domain"),
        PROVIDER_MERIL_SCIENTIFIC_SUBDOMAIN("Provider meril scientific subdomain"),
        // Service
        SUPERCATEGORY("Supercategory"),
        CATEGORY("Category"),
        SUBCATEGORY("Subcategory"),
        LANGUAGE("Language"),
        GEOGRAPHIC_LOCATION("Geographic location"),
        REGION("Region"),
        COUNTRY("Country"),
        TRL("Technology readiness level"),
        SCIENTIFIC_DOMAIN("Scientific domain"),
        SCIENTIFIC_SUBDOMAIN("Scientific subdomain"),
        TARGET_USER("Target user"),
        ACCESS_TYPE("Access type"),
        ACCESS_MODE("Access mode"),
        ORDER_TYPE("Order type"),
        FUNDING_BODY("Funding body"),
        FUNDING_PROGRAM("Funding program"),
        LIFE_CYCLE_STATUS("Life cycle status"),
        RELATED_PLATFORM("Related platform"),
        // States
        CATALOGUE_STATE("Catalogue state"),
        PROVIDER_STATE("Provider state"),
        RESOURCE_STATE("Resource state"),
        TEMPLATE_STATE("Template state"),
        // Datasource
        DS_RESEARCH_ENTITY_TYPE("Research entity type"),
        DS_PERSISTENT_IDENTITY_SCHEME("Persistent identity scheme"),
        DS_JURISDICTION("Jurisdiction"),
        DS_CLASSIFICATION("Classification"),
        DS_COAR_ACCESS_RIGHTS_1_0("COAR access rights 1.0"),
        // Training Resource
        TR_URL_TYPE("Training Resource url type"),
        TR_ACCESS_RIGHT("Training Resource access right"),
        TR_DCMI_TYPE("Training Resource dcmi type"),
        TR_EXPERTISE_LEVEL("Training Resource expertise level"),
        TR_CONTENT_RESOURCE_TYPE("Training Resource content resource type"),
        TR_QUALIFICATION("Training Resource qualification"),
        // Monitoring
        MONITORING_MONITORED_BY("Monitored by"),
        // Bundle Extras
        SEMANTIC_RELATIONSHIP("Semantic relationship"),
        RESEARCH_CATEGORY("Research category"),
        // Interoperability Record
        IR_IDENTIFIER_TYPE("Interoperability Record identifier type"),
        IR_NAME_TYPE("Interoperability Record name type"),
        IR_RESOURCE_TYPE_GENERAL("Interoperability Record resource type general"),
        IR_STATUS("Interoperability Record status"),
        IR_EOSC_GUIDELINE_TYPE("Interoperability Record eosc guideline type"),
        // serviceType
        SERVICE_TYPE("Service type");

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

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getExtras() {
        return extras;
    }

    public void setExtras(Map<String, String> extras) {
        this.extras = extras;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vocabulary that = (Vocabulary) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(parentId, that.parentId) && Objects.equals(type, that.type) && Objects.equals(extras, that.extras);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, parentId, type, extras);
    }
}
