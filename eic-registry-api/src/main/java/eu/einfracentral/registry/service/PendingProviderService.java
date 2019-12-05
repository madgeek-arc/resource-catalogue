package eu.einfracentral.registry.service;

import eu.einfracentral.domain.ProviderBundle;
import org.springframework.security.core.Authentication;

public interface PendingProviderService extends ResourceService<ProviderBundle, Authentication> {

    /**
     * Transforms the Provider to PendingProvider
     *
     * @param providerId
     */
    void transformToPendingProvider(String providerId);

    /**
     * Transforms the PendingProvider to Provider
     *
     * @param providerId
     */
    void transformToActiveProvider(String providerId);

}
