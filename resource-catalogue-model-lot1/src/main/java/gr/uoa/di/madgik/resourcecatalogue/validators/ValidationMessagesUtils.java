package gr.uoa.di.madgik.resourcecatalogue.validators;

public class ValidationMessagesUtils {

    public static String mandatoryField(String field) {
        return String.format("Field '%s' is mandatory", field);
    }

    private ValidationMessagesUtils() {
    }
}
