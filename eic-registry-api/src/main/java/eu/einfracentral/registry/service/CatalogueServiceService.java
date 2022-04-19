package eu.einfracentral.registry.service;

import eu.einfracentral.domain.InfraService;
import org.springframework.security.core.Authentication;

public interface CatalogueServiceService<T, U extends Authentication> extends ResourceService<T, Authentication> {

    /**
     * @param catalogueId
     * @param serviceId
     * @param auth
     * @return
     */
    InfraService getCatalogueService(String catalogueId, String serviceId, Authentication auth);

    /**
     * @param service
     * @param catalogueId
     * @param auth
     * @return
     */
    InfraService addCatalogueService(InfraService service, String catalogueId, Authentication auth);

    /**
     * @param service
     * @param catalogueId
     * @param comment
     * @param auth
     * @return
     */
    InfraService updateCatalogueService(InfraService service, String catalogueId, String comment, Authentication auth);
}
