package eu.einfracentral.utils;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.exception.ValidationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;

public class ServiceValidators {

    private static final Logger logger = LogManager.getLogger(ServiceValidators.class);

    //validates the correctness of Service Name.
    public static void validateName(InfraService service) {
        if (service.getName() == null || service.getName().equals("")) {
            throw new ValidationException("field 'name' is obligatory", HttpStatus.CONFLICT);
        }
        //TODO: Core should check the max length
        if (service.getName().length() > 80) {
            throw new ValidationException("max length for 'name' is 80 chars", HttpStatus.CONFLICT);
        }
    }

    //validates the correctness of Service URL.
    public static void validateURL(InfraService service) {
        if (service.getUrl() == null || service.getUrl().equals("")) {
            throw new ValidationException("field 'url' is mandatory", HttpStatus.CONFLICT);
        }
    }

    //validates the correctness of Service Description.
    public static void validateDescription(InfraService service) {
        if (service.getDescription() == null || service.getDescription().equals("")) {
            throw new ValidationException("field 'description' is mandatory", HttpStatus.CONFLICT);
        }
        //TODO: Core should check the max length
        if (service.getDescription().length() > 1000) {
            throw new ValidationException("max length for 'description' is 1000 chars", HttpStatus.CONFLICT);
        }
    }

    //validates the correctness of Service Symbol.
    public static void validateSymbol(InfraService service) {
        if (service.getSymbol() == null || service.getSymbol().equals("")) {
            throw new ValidationException("field 'symbol' is mandatory", HttpStatus.CONFLICT);
        }
    }

    //validates the correctness of Service Version.
    public static void validateVersion(InfraService service) {
        if (service.getVersion() == null || service.getVersion().equals("")) {
            throw new ValidationException("field 'version' is mandatory", HttpStatus.CONFLICT);
        }
        //TODO: Core should check the max length
        if (service.getVersion().length() > 10) {
            throw new ValidationException("max length for 'version' is 10 chars", HttpStatus.CONFLICT);
        }
    }

    //validates the correctness of Service Last Update (Revision Date).
    public static void validateLastUpdate(InfraService service) {
        if (service.getLastUpdate() == null || service.getLastUpdate().equals("")) {
            throw new ValidationException("field 'Revision Date' (lastUpdate) is mandatory", HttpStatus.CONFLICT);
        }
    }

    //validates the correctness of URL for requesting the service from the service providers.
    public static void validateOrder(InfraService service) {
        if (service.getOrder() == null || service.getOrder().equals("")) {
            throw new ValidationException("field 'order' is mandatory", HttpStatus.CONFLICT);
        }
    }

    //validates the correctness of Service SLA.
    public static void validateSLA(InfraService service) {
        if (service.getServiceLevelAgreement() == null || service.getServiceLevelAgreement().equals("")) {
            throw new ValidationException("field 'serviceLevelAgreement' is mandatory", HttpStatus.CONFLICT);
        }
    }

    //validates the max length of various variables.
    //FIXME: Core should check the max length
    public static void validateMaxLength(InfraService service) {
        if (service.getTagline() != null && service.getTagline().length() > 100) {
            throw new ValidationException("max length for 'tagline' is 100 chars", HttpStatus.CONFLICT);
        }
        if (service.getOptions() != null && service.getOptions().length() > 1000) {
            throw new ValidationException("max length for 'options' is 1000 chars", HttpStatus.CONFLICT);
        }
        if (service.getTargetUsers() != null && service.getTargetUsers().length() > 1000) {
            throw new ValidationException("max length for 'targetUsers' is 1000 chars", HttpStatus.CONFLICT);
        }
        if (service.getUserValue() != null && service.getUserValue().length() > 1000) {
            throw new ValidationException("max length for 'userValue' is 1000 chars", HttpStatus.CONFLICT);
        }
        if (service.getUserBase() != null && service.getUserBase().length() > 1000) {
            throw new ValidationException("max length for 'userBase' is 1000 chars", HttpStatus.CONFLICT);
        }
        if (service.getChangeLog() != null && service.getChangeLog().length() > 1000) {
            throw new ValidationException("max length for 'changeLog' is 1000 chars", HttpStatus.CONFLICT);
        }
        if (service.getFunding() != null && service.getFunding().length() > 500) {
            throw new ValidationException("max length for 'funding' is 500 chars", HttpStatus.CONFLICT);
        }
    }
}
