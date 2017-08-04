package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Provider;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 26/07/17.
 */
@Service("providerService")
public interface ProviderService extends ResourceCRUDService<Provider> {
}
