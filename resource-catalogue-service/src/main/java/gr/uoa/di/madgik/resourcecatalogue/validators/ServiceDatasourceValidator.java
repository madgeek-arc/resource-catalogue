/**
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

import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.domain.Service;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.utils.VocabularyValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.List;

@Component("serviceValidator")
public class ServiceDatasourceValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(ServiceDatasourceValidator.class);
    private final ServiceBundleService serviceBundleService;
    private final String catalogueName;

    public ServiceDatasourceValidator(@Value("${catalogue.id}") String catalogueName,
                                      @Lazy ServiceBundleService serviceBundleService) {
        this.catalogueName = catalogueName;
        this.serviceBundleService = serviceBundleService;
    }


    @Override
    public boolean supports(Class<?> clazz) {
        return ServiceBundle.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Service service = ((ServiceBundle) target).getPayload();
        //TODO: Probably not needed -> related/required field validation was added on FieldValidator
//        if (service.getRelatedResources() != null && !service.getRelatedResources().isEmpty()){
//            validateRelatedResources(service, errors);
//        }
//        if (service.getRequiredResources() != null && !service.getRequiredResources().isEmpty()){
//            validateRequiredResources(service, errors);
//        }
        VocabularyValidationUtils.validateCategories(service.getCategories());
        VocabularyValidationUtils.validateScientificDomains(service.getScientificDomains());
    }

    private void validateRequiredResources(Service service, Errors errors) {
        validateServiceResources(service.getRelatedResources(), service.getCatalogueId(), "relatedResources", errors);
    }

    private void validateRelatedResources(Service service, Errors errors) {
        validateServiceResources(service.getRequiredResources(), service.getCatalogueId(), "requiredResources", errors);
    }


    private void validateServiceResources(List<String> resources, String catalogue, String field, Errors errors) {
        for (String resource : resources) {
            try {
                serviceBundleService.get(resource, catalogue);
            } catch (Exception e) {
                try {
                    serviceBundleService.get(resource, catalogueName);
                } catch (Exception ex) {
//                    errors.rejectValue("relatedResources", field + ".invalid", String.format("Could not find Service with id [%s] in Catalogue [%s]", resource, catalogue));
                    throw new ValidationException(String.format("Could not find Service with id [%s] in Catalogue [%s]", resource, catalogue));
                }
            }
        }
    }
}
