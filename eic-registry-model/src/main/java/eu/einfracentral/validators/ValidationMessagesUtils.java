package eu.einfracentral.validators;

public class ValidationMessagesUtils {

    public static String mandatoryField(String field) {
        return String.format("Field '%s' is mandatory", field);
    }

    private ValidationMessagesUtils() {}
}
