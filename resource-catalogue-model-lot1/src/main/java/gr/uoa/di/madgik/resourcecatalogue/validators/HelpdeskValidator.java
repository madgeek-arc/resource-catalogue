package gr.uoa.di.madgik.resourcecatalogue.validators;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import gr.uoa.di.madgik.resourcecatalogue.domain.Helpdesk;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static gr.uoa.di.madgik.resourcecatalogue.validators.ValidationMessagesUtils.mandatoryField;

public class HelpdeskValidator implements Validator {

    private static final Logger logger = LogManager.getLogger(HelpdeskValidator.class);

    @Override
    public boolean supports(Class<?> clazz) {
        return Helpdesk.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ValidationUtils.rejectIfEmpty(errors, "serviceId", "serviceId.empty", mandatoryField("serviceId"));

        Helpdesk helpdesk = (Helpdesk) target;
        switch (Helpdesk.HelpdeskType.fromString(helpdesk.getHelpdeskType())) {
            case DIRECT_USAGE:
                // Check for unwanted fields: ticketPreservation
                rejectNotAcceptableField(target, errors, "ticketPreservation", helpdesk.getHelpdeskType());

                // Check for mandatory fields: group, agents
                ValidationUtils.rejectIfEmpty(errors, "supportGroups", "supportGroups.empty", mandatoryField("supportGroups"));
                ValidationUtils.rejectIfEmpty(errors, "agents", "agents.empty", mandatoryField("agents"));
                break;

            case FULL_INTEGRATION:
                // Check for unwanted fields: ticketPreservation
                rejectNotAcceptableField(target, errors, "ticketPreservation", helpdesk.getHelpdeskType());
                rejectNotAcceptableField(target, errors, "signatures", helpdesk.getHelpdeskType());

                // Check for mandatory fields: group, agents
                ValidationUtils.rejectIfEmpty(errors, "supportGroups", "supportGroups.empty", mandatoryField("supportGroups"));
                ValidationUtils.rejectIfEmpty(errors, "agents", "agents.empty", mandatoryField("agents"));
                break;

            case TICKET_REDIRECTION:
                // Check for unwanted fields: supportGroups, agents, signatures
                rejectNotAcceptableField(target, errors, "supportGroups", helpdesk.getHelpdeskType());
                rejectNotAcceptableField(target, errors, "agents", helpdesk.getHelpdeskType());
                rejectNotAcceptableField(target, errors, "signatures", helpdesk.getHelpdeskType());

                // Check for mandatory fields: email
                ValidationUtils.rejectIfEmpty(errors, "emails", "emails.empty", mandatoryField("emails"));
                break;
            default:
        }
    }

    public void rejectNotAcceptableField(Object target, Errors errors, String fieldName, String type) {
        Field field = null;
        try {
            field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object fieldValue = field.get(target);
            if (fieldValue != null && !fieldValue.equals("")) {
                if (fieldValue instanceof ArrayList) { // front may return a 0 items list (non-empty)
                    if (!((ArrayList<?>) fieldValue).isEmpty()) {
                        errors.rejectValue(fieldName, fieldName + ".non-empty", notAcceptableField(fieldName, type));
                    }
                } else {
                    errors.rejectValue(fieldName, fieldName + ".non-empty", notAcceptableField(fieldName, type));
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error(e);
        }
    }

    public static String notAcceptableField(String field, String serviceType) {
        return String.format("Field '%s' is not acceptable for serviceType '%s'", field, serviceType);
    }
}
