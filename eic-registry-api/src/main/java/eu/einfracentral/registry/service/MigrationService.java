package eu.einfracentral.registry.service;

import eu.einfracentral.domain.ProviderBundle;
import org.springframework.security.core.Authentication;

public interface MigrationService {

    ProviderBundle changeProviderCatalogue(String providerId, String catalogueId, String newCatalogueId, Authentication authentication);
}
