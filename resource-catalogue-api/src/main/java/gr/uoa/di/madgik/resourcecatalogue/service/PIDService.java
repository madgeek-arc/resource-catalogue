package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;

public interface PIDService {

    /**
     * Get a Resource via its PID
     *
     * @param prefix PID prefix
     * @param suffix PID suffix
     * @return Bundle<?>
     */
    Bundle<?> get(String prefix, String suffix);

    /**
     * Registers a PID on a specific resource
     *
     * @param pid PID
     */
    void register(String pid);
}
