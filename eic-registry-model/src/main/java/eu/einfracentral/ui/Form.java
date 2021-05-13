package eu.einfracentral.ui;

public class Form {

    Type type;
    String vocabulary; //(will have value if type==vocabulary)
    String group;
    String subgroup;
    String description;
    String suggestion;
    String placeholder;
    boolean mandatory = false;
    boolean multiplicity = false;

    public Form() {
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setType(String type) {
        this.type = Type.valueOf(type);
    }

    public String getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(String vocabulary) {
        this.vocabulary = vocabulary;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getSubgroup() {
        return subgroup;
    }

    public void setSubgroup(String subgroup) {
        this.subgroup = subgroup;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    public Boolean getMultiplicity() {
        return multiplicity;
    }

    public void setMultiplicity(Boolean multiplicity) {
        this.multiplicity = multiplicity;
    }


    public enum Type {
        STRING                  ("string"),
        URL                     ("string"),
        INT                     ("int"),
        BOOLEAN                 ("boolean"),
        LIST                    ("array"),
        VOCABULARY              ("vocabulary"),
        SERVICEPROVIDERDOMAIN   ("ServiceProviderDomain"),
        SERVICECATEGORY         ("ServiceCategory"),
        SERVICEMAINCONTACT      ("ServiceMainContact"),
        SERVICEPUBLICCONTACT    ("ServicePublicContact"),
        XMLGREGORIANCALENDAR    ("date");

        private final String typeValue;

        Type(final String type) {
            this.typeValue = type;
        }
    }
}
