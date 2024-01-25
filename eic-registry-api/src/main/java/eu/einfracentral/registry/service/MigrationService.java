package eu.einfracentral.registry.service;

import eu.einfracentral.domain.ProviderBundle;
import org.springframework.security.core.Authentication;

public interface MigrationService {
    /**
     * Migrate a Provider and all its resources to another Catalogue
     *
     * @param providerId     Provider ID
     * @param catalogueId    The Catalogue ID in which the Provider is registered
     * @param newCatalogueId The new Catalogue ID in which the Provider will be registered
     * @param authentication Authentication
     * @return {@link ProviderBundle}
     */
    ProviderBundle changeProviderCatalogue(String providerId, String catalogueId, String newCatalogueId,
                                           Authentication authentication);

    /**
     * Update all Project's resources' fields related to a specific resource ID when migrating this resource to another
     * Provider
     *
     * @param oldResourceId The old resource ID
     * @param newResourceId The new resource ID
     */
    void updateRelatedToTheIdFieldsOfOtherResourcesOfThePortal(String oldResourceId, String newResourceId);
}
