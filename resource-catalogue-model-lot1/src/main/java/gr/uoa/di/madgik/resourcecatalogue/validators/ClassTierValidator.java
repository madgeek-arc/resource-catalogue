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

import gr.uoa.di.madgik.resourcecatalogue.annotation.ClassTierValidation;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceClassTier;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ClassTierValidator implements ConstraintValidator<ClassTierValidation, ServiceClassTier> {

    @Override
    public boolean isValid(ServiceClassTier value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Consider null valid or handle according to your needs
        }

        int level = value.getLevel();
        String accessPolicy = value.getAccessPolicy();
        String costModel = value.getCostModel();

        // Validate that level is either 1, 2, or 3
        if (level != 1 && level != 2 && level != 3) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Level must be either 1, 2, or 3")
                    .addConstraintViolation();
            return false;
        }

        // Validate accessPolicy and costModel based on level
        if (level == 3) {
            return true;
        } else if ((level == 1 || level == 2) && (accessPolicy == null || accessPolicy.isEmpty() || costModel == null || costModel.isEmpty())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("AccessPolicy and CostModel are required for levels 1 and 2")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}

