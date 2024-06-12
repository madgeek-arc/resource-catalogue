package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;

public interface PIDService {

    /**
     * Get a Resource via its PID
     *
     * @param pid PID
     * @return Bundle<?>
     */
    Bundle<?> get(String pid);

    /**
     * Registers a PID on a specific resource
     *
     * @param pid PID
     */
    void register(String pid);
}
