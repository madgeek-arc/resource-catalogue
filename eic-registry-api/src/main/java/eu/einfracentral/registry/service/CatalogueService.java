package eu.einfracentral.registry.service;

import eu.einfracentral.domain.CatalogueBundle;
import eu.einfracentral.domain.ProviderBundle;
import org.springframework.security.core.Authentication;

public interface CatalogueService<T, U extends Authentication> extends ResourceService<T, Authentication> {

    T get(String id, U auth);

    /**
     * @param catalogue
     * @param comment
     * @param auth
     * @return
     */
    CatalogueBundle update(CatalogueBundle catalogue, String comment, Authentication auth);
}
