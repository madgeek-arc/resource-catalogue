package eu.einfracentral.service;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.registry.manager.ProviderManager;
import eu.einfracentral.utils.AuthenticationDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service("securityService")
public class SecurityService {

    private ProviderManager providerManager;

    @Autowired
    SecurityService(ProviderManager providerManager) {
        this.providerManager = providerManager;
    }

    public boolean providerIsActive(Authentication auth, Provider provider) throws Exception {
        Provider registeredProvider = providerManager.get(provider.getId());
        String email = AuthenticationDetails.getEmail(auth);
        if (registeredProvider == null) {
            throw new Exception("Provider with id '" + provider.getId() + "' does not exist.");
        }
        return registeredProvider.getActive() &&
                registeredProvider.getUsers().parallelStream().anyMatch(s -> s.getEmail().equals(email));
    }

    public boolean userIsServiceProvider(Authentication auth, eu.einfracentral.domain.Service service) throws Exception {
        String email = AuthenticationDetails.getEmail(auth);
        Optional<List<String>> providers = Optional.of(service.getProviders());
        return providers
                .get()
                .stream()
                .map(id -> providerManager.get(id))
                .flatMap(x -> x.getUsers().stream().filter(Objects::nonNull))
                .anyMatch(x -> x.getEmail().equals(email));
    }
}
