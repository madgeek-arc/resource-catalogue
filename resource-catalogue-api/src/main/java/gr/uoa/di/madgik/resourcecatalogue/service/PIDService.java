package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;

public interface PIDService {

    /**
     * Get a Resource via its PID
     *
     * @param pid          PID
     * @param resourceType Resource Type
     * @return Bundle<?>
     */
    Bundle<?> get(String pid, String resourceType);

    /**
     * Update the PID of a specific Public resource
     *
     * @param pid              Resource's PID
     * @param resourceTypePath Resource's type needed to create MP URL
     */
    void updatePID(String pid, String resourceTypePath);

    /**
     * Given a resourceType, determine the url path the Resource needs to be redirected to when resolving on handler
     *
     * @param resourceType Resource's type needed to create MP URL
     * @return String
     */
    String determineResourceTypePath(String resourceType);
}
