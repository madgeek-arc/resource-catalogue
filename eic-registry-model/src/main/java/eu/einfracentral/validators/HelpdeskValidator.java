package eu.einfracentral.validators;

import eu.einfracentral.domain.Helpdesk;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import static eu.einfracentral.validators.ValidationMessagesUtils.mandatoryField;

public class HelpdeskValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return Helpdesk.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ValidationUtils.rejectIfEmpty(errors, "services", "services.empty", mandatoryField("services"));

        Helpdesk helpdesk = (Helpdesk) target;
        switch (Helpdesk.HelpdeskType.fromString(helpdesk.getHelpdeskType())) {
            case DIRECT_USAGE:
            case FULL_INTEGRATION:
                // group
                ValidationUtils.rejectIfEmpty(errors, "supportGroups", "supportGroups.empty", mandatoryField("supportGroups"));
                // agent
                ValidationUtils.rejectIfEmpty(errors, "agents", "agents.empty", mandatoryField("agents"));
                break;
            case TICKET_REDIRECTION:
                // email
                ValidationUtils.rejectIfEmpty(errors, "emails", "emails.empty", mandatoryField("emails"));
                break;
            default:
        }
    }
}
