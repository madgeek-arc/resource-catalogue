package eu.einfracentral.service;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.registry.manager.ProviderManager;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.utils.AuthenticationInfo;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service("securityService")
public class SecurityService {

    private ProviderManager providerManager;
    private InfraServiceService<InfraService, InfraService> infraServiceService;

    @Autowired
    SecurityService(ProviderManager providerManager, InfraServiceService<InfraService, InfraService> infraServiceService) {
        this.providerManager = providerManager;
        this.infraServiceService = infraServiceService;
    }

    public boolean userIsProviderAdmin(Authentication auth, Provider provider) {
        Provider registeredProvider = providerManager.get(provider.getId());
        String email = AuthenticationInfo.getEmail(auth);
        if (registeredProvider == null) {
            throw new ServiceException("Provider with id '" + provider.getId() + "' does not exist.");
        } else if (!registeredProvider.getActive()) {
            throw new ServiceException("Provider is not active");
        }
        return registeredProvider.getActive() &&
                registeredProvider.getUsers().parallelStream().anyMatch(s -> s.getEmail().equals(email));
    }

    public boolean userIsProviderAdmin(Authentication auth, String providerId) {
        Provider registeredProvider = providerManager.get(providerId);
        String email = AuthenticationInfo.getEmail(auth);
        if (registeredProvider == null) {
            throw new ServiceException("Provider with id '" + providerId + "' does not exist.");
        } else if (!registeredProvider.getActive()) {
            throw new ServiceException("Provider is not active");
        }
        return registeredProvider.getActive() &&
                registeredProvider.getUsers().parallelStream().anyMatch(s -> s.getEmail().equals(email));
    }

    public boolean userIsServiceProviderAdmin(Authentication auth, eu.einfracentral.domain.Service service) throws ResourceNotFoundException {
        String email = AuthenticationInfo.getEmail(auth);
        service = infraServiceService.getLatest(service.getId());
        Optional<List<String>> providers = Optional.of(service.getProviders());
        return providers
                .get()
                .stream()
                .map(id -> providerManager.get(id))
                .flatMap(x -> x.getUsers().stream().filter(Objects::nonNull))
                .anyMatch(x -> x.getEmail().equals(email));
    }

    public boolean userIsServiceProviderAdmin(Authentication auth, String serviceId) throws ResourceNotFoundException {
        String email = AuthenticationInfo.getEmail(auth);
        InfraService service = infraServiceService.getLatest(serviceId);
        Optional<List<String>> providers = Optional.of(service.getProviders());
        return providers
                .get()
                .stream()
                .map(id -> providerManager.get(id))
                .flatMap(x -> x.getUsers().stream().filter(Objects::nonNull))
                .anyMatch(x -> x.getEmail().equals(email));
    }
}
