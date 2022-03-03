package eu.einfracentral.registry.service;

import eu.einfracentral.domain.CatalogueBundle;
import eu.einfracentral.domain.ServiceProviderDomain;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface CatalogueService<T, U extends Authentication> extends ResourceService<T, Authentication> {

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

    void validateScientificDomains(List<ServiceProviderDomain> scientificDomains);
}
