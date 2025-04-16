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

import gr.uoa.di.madgik.resourcecatalogue.domain.Monitoring;
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
