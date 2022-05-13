package eu.einfracentral.utils;

import eu.einfracentral.domain.ProviderMerilDomain;
import eu.einfracentral.domain.ServiceCategory;
import eu.einfracentral.domain.ServiceProviderDomain;
import eu.einfracentral.exception.ValidationException;

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

    private VocabularyValidationUtils() {}
}
