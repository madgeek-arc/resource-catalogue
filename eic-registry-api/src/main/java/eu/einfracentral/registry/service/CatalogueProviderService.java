package eu.einfracentral.registry.service;

import eu.einfracentral.domain.ProviderBundle;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import org.springframework.security.core.Authentication;

public interface CatalogueProviderService<T, U extends Authentication> extends ResourceService<T, Authentication> {

    /**
     * @param catalogueId
     * @param providerId
     * @param auth
     * @return
     */
    ProviderBundle getCatalogueProvider(String catalogueId, String providerId, Authentication auth);

    /**
     * @param provider
     * @param catalogueId
     * @param auth
     * @return
     */
    ProviderBundle addCatalogueProvider(ProviderBundle provider, String catalogueId, Authentication auth);

    /**
     * @param provider
     * @param catalogueId
     * @param comment
     * @param auth
     * @return
     */
    ProviderBundle updateCatalogueProvider(ProviderBundle provider, String catalogueId, String comment, Authentication auth);

    /**
     * @param ff
     * @param auth
     * @return
     */
    Browsing<ProviderBundle> getAllCatalogueProviders(FacetFilter ff, Authentication auth);
}
