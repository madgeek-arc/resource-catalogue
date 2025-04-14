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

package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

public class ResourceValidationUtils {

    public static <T extends ServiceBundle> void checkIfResourceBundleIsActiveAndApprovedAndNotPublic(String resourceId, String catalogueId,
                                                                                                      ServiceBundleService<T> serviceBundleService,
                                                                                                      String resourceType) {
        T resourceBundle;
        resourceType = StringUtils.capitalize(resourceType);
        // check if Resource exists
        try {
            resourceBundle = serviceBundleService.get(resourceId);
            // check if Service is Public
            if (resourceBundle.getMetadata().isPublished()) {
                throw new ValidationException(String.format("Please provide a %s ID with no catalogue prefix.", resourceType));
            }
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(String.format("There is no %s with id '%s' in the '%s' Catalogue",
                    resourceType, resourceId, catalogueId));
        }
        // check if Service is Active + Approved
        if (!resourceBundle.isActive() || !resourceBundle.getStatus().equals("approved resource")) {
            throw new ResourceException(String.format("%s with ID '%s' is not Approved and/or Active",
                    resourceType, resourceId), HttpStatus.CONFLICT);
        }
    }

    public static void checkIfResourceBundleIsActiveAndApprovedAndNotPublic(String resourceId, String catalogueId,
                                                                            TrainingResourceService trainingResourceService,
                                                                            String resourceType) {
        TrainingResourceBundle trainingResourceBundle;
        resourceType = StringUtils.capitalize(resourceType);
        // check if Resource exists
        try {
            trainingResourceBundle = trainingResourceService.get(resourceId, catalogueId);
            // check if Service is Public
            if (trainingResourceBundle.getMetadata().isPublished()) {
                throw new ValidationException(String.format("Please provide a %s ID with no catalogue prefix.", resourceType));
            }
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(String.format("There is no %s with id '%s' in the '%s' Catalogue",
                    resourceType, resourceId, catalogueId));
        }
        // check if TR is Active + Approved
        if (!trainingResourceBundle.isActive() || !trainingResourceBundle.getStatus().equals("approved resource")) {
            throw new ResourceException(String.format("%s with ID '%s' is not Approved and/or Active",
                    resourceType, resourceId), HttpStatus.CONFLICT);
        }
    }

    private ResourceValidationUtils() {
    }
}
