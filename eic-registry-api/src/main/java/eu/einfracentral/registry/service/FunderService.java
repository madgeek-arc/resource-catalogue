package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Funder;
import org.springframework.security.core.Authentication;

import java.util.Map;

public interface FunderService extends ResourceService<Funder, Authentication> {

    /**
     * Returns various stats about the specific Funder
     *
     * @param funderId
     * @param auth
     * @return
     */
    Map<String, Map<String, Double>> getFunderStats(String funderId, Authentication auth);
}
