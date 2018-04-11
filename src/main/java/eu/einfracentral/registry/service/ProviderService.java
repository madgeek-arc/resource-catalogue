package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Provider;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 26/07/17.
 */
@Service("providerService")
public interface ProviderService extends ResourceService<Provider> {
    Map<String, Integer> visits(String id);
    Map<String, Integer> favourites(String id);
    Map<String, Float> ratings(String id);
    Map<String, Float> visitation(String id);
}
