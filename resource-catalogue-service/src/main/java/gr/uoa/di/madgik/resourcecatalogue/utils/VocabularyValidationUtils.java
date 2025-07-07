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
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderMerilDomain;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceCategory;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceProviderDomain;

import java.util.List;

public class VocabularyValidationUtils {

    /**
     * Validates Scientific Domains/Subdomains
     *
     * @param scientificDomains
     */
    public static void validateScientificDomains(List<ServiceProviderDomain> scientificDomains) {
        for (ServiceProviderDomain providerScientificDomain : scientificDomains) {
            String[] parts = providerScientificDomain.getScientificSubdomain().split("-");
            String scientificDomain = "scientific_domain-" + parts[1];
            if (!providerScientificDomain.getScientificDomain().equals(scientificDomain)) {
                throw new ValidationException("Scientific Subdomain '" + providerScientificDomain.getScientificSubdomain() +
                        "' should have as Scientific Domain the value '" + scientificDomain + "'");
            }
        }
    }

    /**
     * Validates Meril Scientific Domains/Subdomains
     *
     * @param merilScientificDomains
     */
    public static void validateMerilScientificDomains(List<ProviderMerilDomain> merilScientificDomains) {
        for (ProviderMerilDomain providerMerilScientificDomain : merilScientificDomains) {
            String[] parts = providerMerilScientificDomain.getMerilScientificSubdomain().split("-");
            String merilScientificDomain = "provider_meril_scientific_domain-" + parts[1];
            if (!providerMerilScientificDomain.getMerilScientificDomain().equals(merilScientificDomain)) {
                throw new ValidationException("Meril Scientific Subdomain '" + providerMerilScientificDomain.getMerilScientificSubdomain() +
                        "' should have as Meril Scientific Domain the value '" + merilScientificDomain + "'");
            }
        }
    }

    /**
     * Validates Categories/Subcategories
     *
     * @param categories
     */
    public static void validateCategories(List<ServiceCategory> categories) {
        for (ServiceCategory serviceCategory : categories) {
            String[] parts = serviceCategory.getSubcategory().split("-");
            String category = "category-" + parts[1] + "-" + parts[2];
            if (!serviceCategory.getCategory().equals(category)) {
                throw new ValidationException("Subcategory '" + serviceCategory.getSubcategory() + "' should have as Category the value '"
                        + category + "'");
            }
        }
    }

    private VocabularyValidationUtils() {
    }
}
