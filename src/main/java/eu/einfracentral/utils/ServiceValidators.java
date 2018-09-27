package eu.einfracentral.utils;

import eu.einfracentral.domain.InfraService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServiceValidators {

    private static final Logger logger = LogManager.getLogger(ServiceValidators.class);

    //validates the correctness of Service Name.
    public static void validateName(InfraService service) throws Exception {
        if (service.getName() == null || service.getName().equals("")) {
            throw new Exception("field 'name' is obligatory");
        }
        //TODO: Core should check the max length
        if (service.getName().length() > 80) {
            throw new Exception("max length for 'name' is 80 chars");
        }
    }

    //validates the correctness of Service URL.
    public static void validateURL(InfraService service) throws Exception {
        if (service.getUrl() == null || service.getUrl().equals("")) {
            throw new Exception("field 'url' is mandatory");
        }
    }

    //validates the correctness of Service Description.
    public static void validateDescription(InfraService service) throws Exception {
        if (service.getDescription() == null || service.getDescription().equals("")) {
            throw new Exception("field 'description' is mandatory");
        }
        //TODO: Core should check the max length
        if (service.getDescription().length() > 1000) {
            throw new Exception("max length for 'description' is 1000 chars");
        }
    }

    //validates the correctness of Service Symbol.
    public static void validateSymbol(InfraService service) throws Exception {
        if (service.getSymbol() == null || service.getSymbol().equals("")) {
            throw new Exception("field 'symbol' is mandatory");
        }
    }

    //validates the correctness of Service Version.
    public static void validateVersion(InfraService service) throws Exception {
        if (service.getVersion() == null || service.getVersion().equals("")) {
            throw new Exception("field 'version' is mandatory");
        }
        //TODO: Core should check the max length
        if (service.getVersion().length() > 10) {
            throw new Exception("max length for 'version' is 10 chars");
        }
    }

    //validates the correctness of Service Last Update (Revision Date).
    public static void validateLastUpdate(InfraService service) throws Exception {
        if (service.getLastUpdate() == null || service.getLastUpdate().equals("")) {
            throw new Exception("field 'Revision Date' (lastUpdate) is mandatory");
        }
    }

    //validates the correctness of URL for requesting the service from the service providers.
    public static void validateOrder(InfraService service) throws Exception {
        if (service.getOrder() == null || service.getOrder().equals("")) {
            throw new Exception("field 'order' is mandatory");
        }
    }

    //validates the correctness of Service SLA.
    public static void validateSLA(InfraService service) throws Exception {
        if (service.getServiceLevelAgreement() == null || service.getServiceLevelAgreement().equals("")) {
            throw new Exception("field 'serviceLevelAgreement' is mandatory");
        }
    }

    //validates the max length of various variables.
    //FIXME: Core should check the max length
    public static void validateMaxLength(InfraService service) throws Exception {
        if (service.getTagline() != null && service.getTagline().length() > 100) {
            throw new Exception("max length for 'tagline' is 100 chars");
        }
        if (service.getOptions() != null && service.getOptions().length() > 1000) {
            throw new Exception("max length for 'options' is 1000 chars");
        }
        if (service.getTargetUsers() != null && service.getTargetUsers().length() > 1000) {
            throw new Exception("max length for 'targetUsers' is 1000 chars");
        }
        if (service.getUserValue() != null && service.getUserValue().length() > 1000) {
            throw new Exception("max length for 'userValue' is 1000 chars");
        }
        if (service.getUserBase() != null && service.getUserBase().length() > 1000) {
            throw new Exception("max length for 'userBase' is 1000 chars");
        }
        if (service.getChangeLog() != null && service.getChangeLog().length() > 1000) {
            throw new Exception("max length for 'changeLog' is 1000 chars");
        }
        if (service.getFunding() != null && service.getFunding().length() > 500) {
            throw new Exception("max length for 'funding' is 500 chars");
        }
    }
}
