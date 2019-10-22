package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Funder;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface FunderService extends ResourceService<Funder, Authentication> {

    /**
     * Adds all new funders.
     *
     */
    void addAll(List<Funder> funders, Authentication auth);

    /**
     * Get all funders in a Map.
     *
     * @return
     */
    Map<String, Funder> getFundersMap();

    /**
     * Deletes all Funders.
     *
     */
    void deleteAll(Authentication auth);

}
