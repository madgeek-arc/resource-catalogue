package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Bundle;
import org.springframework.security.core.Authentication;

public interface PendingResourceService<T extends Bundle> extends ResourceService<T, Authentication> {

    /**
     * Transforms the resource to pending.
     *
     * @param id
     */
    void transformToPending(String id);

    /**
     * Transforms the resource to active.
     *
     * @param id
     */
    void transformToActive(String id);

}
