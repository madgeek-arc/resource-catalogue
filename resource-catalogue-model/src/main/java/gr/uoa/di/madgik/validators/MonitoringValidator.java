package gr.uoa.di.madgik.validators;

import gr.uoa.di.madgik.domain.Monitoring;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

public class MonitoringValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return Monitoring.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ValidationUtils.rejectIfEmpty(errors, "serviceId", "serviceId.empty", ValidationMessagesUtils.mandatoryField("serviceId"));
//        ValidationUtils.rejectIfEmpty(errors, "monitoredBy", "monitoredBy.empty", mandatoryField("monitoredBy"));
        ValidationUtils.rejectIfEmpty(errors, "monitoringGroups", "monitoringGroups.empty", ValidationMessagesUtils.mandatoryField("monitoringGroups"));
    }
}
