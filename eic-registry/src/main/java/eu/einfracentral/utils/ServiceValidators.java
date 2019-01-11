package eu.einfracentral.utils;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.exception.ValidationException;

public class ServiceValidators {

    private ServiceValidators() {
    }

    //validates the correctness of Service Name.
    public static void validateName(InfraService service) {
        if (service.getName() == null || service.getName().equals("")) {
            throw new ValidationException("field 'name' is obligatory");
        }
        //TODO: Core should check the max length
        // ||||| changed this validation x2 |||||
        if (service.getName().length() > 160) {
            throw new ValidationException("max length for 'name' is 80 chars");
        }
    }

    //validates the correctness of Service URL.
    public static void validateURL(InfraService service) {
        if (service.getUrl() == null || service.getUrl().toString().equals("")) {
            throw new ValidationException("field 'url' is mandatory");
        }
    }

    //validates the correctness of Service Description.
    public static void validateDescription(InfraService service) {
        if (service.getDescription() == null || service.getDescription().equals("")) {
            throw new ValidationException("field 'description' is mandatory");
        }
        //TODO: Core should check the max length
        // ||||| changed this validation x3 |||||
        if (service.getDescription().length() > 3000) {
            throw new ValidationException("max length for 'description' is 1000 chars");
        }
    }

    //validates the correctness of Service Symbol.
    public static void validateSymbol(InfraService service) {
        if (service.getSymbol() == null || service.getSymbol().toString().equals("")) {
            throw new ValidationException("field 'symbol' is mandatory");
        }
    }

    //validates the correctness of Service Version.
    public static void validateVersion(InfraService service) {
        if (service.getVersion() == null || service.getVersion().equals("")) {
            throw new ValidationException("field 'version' is mandatory");
        }
        //TODO: Core should check the max length
        // ||||| changed this validation x2 |||||
        if (service.getVersion().length() > 20) {
            throw new ValidationException("max length for 'version' is 10 chars");
        }
    }

    //validates the correctness of Service Last Update (Revision Date).
    public static void validateLastUpdate(InfraService service) {
        if (service.getLastUpdate() == null || service.getLastUpdate().toString().equals("")) {
            throw new ValidationException("field 'Revision Date' (lastUpdate) is mandatory");
        }
    }

    //validates the correctness of URL for requesting the service from the service providers.
    public static void validateOrder(InfraService service) {
        if (service.getOrder() == null || service.getOrder().toString().equals("")) {
            throw new ValidationException("field 'order' is mandatory");
        }
    }

    //validates the correctness of Service SLA.
    public static void validateSLA(InfraService service) {
        if (service.getServiceLevelAgreement() == null || service.getServiceLevelAgreement().toString().equals("")) {
            throw new ValidationException("field 'serviceLevelAgreement' is mandatory");
        }
    }

    //validates the max length of various variables.
    //FIXME: Core should check the max length
    // ||||| changed all validations x3 |||||
    public static void validateMaxLength(InfraService service) {
        if (service.getTagline() != null && service.getTagline().length() > 300) {
            throw new ValidationException("max length for 'tagline' is 100 chars");
        }
        if (service.getOptions() != null && service.getOptions().length() > 3000) {
            throw new ValidationException("max length for 'options' is 1000 chars");
        }
        if (service.getTargetUsers() != null && service.getTargetUsers().length() > 3000) {
            throw new ValidationException("max length for 'targetUsers' is 1000 chars");
        }
        if (service.getUserValue() != null && service.getUserValue().length() > 3000) {
            throw new ValidationException("max length for 'userValue' is 1000 chars");
        }
        if (service.getUserBase() != null && service.getUserBase().length() > 3000) {
            throw new ValidationException("max length for 'userBase' is 1000 chars");
        }
        if (service.getChangeLog() != null && service.getChangeLog().length() > 3000) {
            throw new ValidationException("max length for 'changeLog' is 1000 chars");
        }
        if (service.getFunding() != null && service.getFunding().length() > 1500) {
            throw new ValidationException("max length for 'funding' is 500 chars");
        }
    }
}
