/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.validators;

import gr.uoa.di.madgik.resourcecatalogue.domain.Helpdesk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static gr.uoa.di.madgik.resourcecatalogue.validators.ValidationMessagesUtils.mandatoryField;

public class HelpdeskValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(HelpdeskValidator.class);

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
            logger.error(e.getMessage(), e);
        }
    }

    public static String notAcceptableField(String field, String serviceType) {
        return String.format("Field '%s' is not acceptable for serviceType '%s'", field, serviceType);
    }
}
