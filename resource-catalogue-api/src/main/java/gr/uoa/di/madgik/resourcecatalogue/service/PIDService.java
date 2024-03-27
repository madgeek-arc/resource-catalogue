package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;

public interface PIDService {

    /**
     * Get a Resource via its PID
     *
     * @param resourceType Resource Type
     * @param pid          PID
     * @return Bundle<?>
     */
    Bundle<?> get(String resourceType, String pid);

    /**
     * Update the PID of a specific Public resource
     *
     * @param pid              Resource's PID
     * @param resourceId       Resource's ID
     * @param resourceTypePath Resource's type needed to create MP URL
     */
    void updatePID(String pid, String resourceId, String resourceTypePath);
}
