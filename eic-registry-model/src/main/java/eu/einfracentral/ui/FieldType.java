package eu.einfracentral.ui;

import java.util.Arrays;

public enum FieldType {
    STRING                  ("string"),
    URL                     ("url"),
    INT                     ("int"),
    BOOLEAN                 ("boolean"),
    LIST                    ("array"),
    METADATA                ("Metadata"),
    VOCABULARY              ("vocabulary"),
    COMPOSITE               ("composite"),
    XMLGREGORIANCALENDAR    ("date");

    private final String typeValue;

    FieldType(final String type) {
        this.typeValue = type;
    }

    public String getKey() {
        return typeValue;
    }

    /**
     * @return the Enum representation for the given string.
     * @throws IllegalArgumentException if unknown string.
     */
    public static FieldType fromString(String s) throws IllegalArgumentException {
        return Arrays.stream(FieldType.values())
                .filter(v -> v.typeValue.equalsIgnoreCase(s))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
    }

    /**
     * Checks if the given {@link String} exists in the values of the enum.
     * @return boolean
     */
    public static boolean exists(String s) {
        return Arrays.stream(FieldType.values())
                .anyMatch(v -> v.typeValue.equalsIgnoreCase(s));
    }
}
