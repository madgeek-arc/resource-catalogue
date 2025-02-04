package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;

import java.util.List;

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
     * @param pid       PID
     * @param endpoints List of endpoints in which the specific resource resolves (optional)
     */
    void register(String pid, List<String> endpoints);
}
