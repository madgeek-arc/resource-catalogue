package eu.einfracentral.registry.service;

import eu.einfracentral.domain.InfraService;
import org.springframework.security.core.Authentication;

public interface PendingServiceService extends ResourceService<InfraService, Authentication> {

    /**
     * Transforms the InfraService to PendingService
     *
     * @param serviceId
     */
    void transformToPendingService(String serviceId);

    /**
     * Transforms the InfraService to PendingService
     *
     * @param serviceId
     */
    void transformToInfraService(String serviceId);

}
