package eu.einfracentral.registry.service;

import eu.einfracentral.domain.CatalogueBundle;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.domain.ServiceProviderDomain;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface CatalogueService<T, U extends Authentication> extends ResourceService<T, Authentication> {

    //SECTION: CATALOGUE
    T get(String id, U auth);

    @Override
    T add(T catalogue, Authentication authentication);

    /**
     * @param catalogue
     * @param comment
     * @param auth
     * @return
     */
    CatalogueBundle update(CatalogueBundle catalogue, String comment, Authentication auth);

    List<T> getMyCatalogues(U authentication);

    List<T> getInactive();

    void validateScientificDomains(List<ServiceProviderDomain> scientificDomains);

    T verifyCatalogue(String id, String status, Boolean active, U auth);

    CatalogueBundle publish(String catalogueId, Boolean active, Authentication auth);

    //SECTION: PROVIDER
    /**
     * @param catalogueId
     * @param providerId
     * @param auth
     * @return
     */
    ProviderBundle getCatalogueProvider(String catalogueId, String providerId, Authentication auth);

    /**
     * @param provider
     * @param auth
     * @return
     */
    ProviderBundle addCatalogueProvider(ProviderBundle provider, Authentication auth);
}
