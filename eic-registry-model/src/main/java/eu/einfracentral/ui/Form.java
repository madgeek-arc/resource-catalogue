package eu.einfracentral.ui;

import java.util.Arrays;

public class Form {

    String type;
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

    public String getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type.getKey();
    }

    public void setType(String type) {
        if (Type.exists(type)) {
            this.type = Type.fromString(type).getKey();
        }
        this.type = Type.valueOf(type.toUpperCase()).getKey();
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
        METADATA                ("Metadata"),
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

        public String getKey() {
            return typeValue;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static Type fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(Type.values())
                    .filter(v -> v.typeValue.equalsIgnoreCase(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }

        /**
         * Checks if the given {@link String} exists in the values of the enum.
         * @return boolean
         */
        public static boolean exists(String s) {
            return Arrays.stream(Type.values())
                    .anyMatch(v -> v.typeValue.equalsIgnoreCase(s));
        }
    }
}
