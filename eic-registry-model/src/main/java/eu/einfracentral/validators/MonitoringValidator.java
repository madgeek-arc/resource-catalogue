package eu.einfracentral.validators;

import eu.einfracentral.domain.Monitoring;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import static eu.einfracentral.validators.ValidationMessagesUtils.mandatoryField;

public class MonitoringValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return Monitoring.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ValidationUtils.rejectIfEmpty(errors, "service", "service.empty", mandatoryField("service"));
        ValidationUtils.rejectIfEmpty(errors, "monitoredBy", "monitoredBy.empty", mandatoryField("monitoredBy"));
        ValidationUtils.rejectIfEmpty(errors, "monitoringGroup", "monitoringGroup.empty", mandatoryField("monitoringGroup"));
    }
}
