package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Bundle;

public interface PIDService {

    /**
     * Get a Resource via its PID
     *
     * @param resourceType - Resource Type
     * @param pid          - PID
     * @return Bundle<?>
     */
    Bundle<?> get(String resourceType, String pid);

    void updatePID(String pid, String resourceId, String resourceTypePath);
}
