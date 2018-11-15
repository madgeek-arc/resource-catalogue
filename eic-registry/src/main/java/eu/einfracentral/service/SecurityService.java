package eu.einfracentral.service;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.User;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.manager.ProviderManager;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.utils.AuthenticationInfo;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ServiceException;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("securityService")
public class SecurityService {

    private ProviderManager providerManager;
    private InfraServiceService<InfraService, InfraService> infraServiceService;

    @Autowired
    SecurityService(ProviderManager providerManager, InfraServiceService<InfraService, InfraService> infraServiceService) {
        this.providerManager = providerManager;
        this.infraServiceService = infraServiceService;
    }

    public Authentication getAdminAccess() {
        List<GrantedAuthority> roles = new ArrayList<>();
        roles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        return new OIDCAuthenticationToken("", "", null, roles, null, "", "");
    }

    public boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
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

    public boolean userIsServiceProviderAdmin(Authentication auth, eu.einfracentral.domain.Service service) {
        String email = AuthenticationInfo.getEmail(auth);
        if (service.getProviders().isEmpty()) {
            throw new ValidationException("Service has no providers");
        }
        Optional<List<String>> providers = Optional.of(service.getProviders());
        return providers
                .get()
                .stream()
                .map(id -> providerManager.get(id))
                .flatMap(x -> x.getUsers().stream().filter(Objects::nonNull))
                .anyMatch(x -> x.getEmail().equals(email));
    }

    public boolean providerCanAddServices(Authentication auth, InfraService service) {
        User user = new User(auth);

        List<String> providerNames = service.getProviders();
        for (String providerName : providerNames) {
            Provider provider = providerManager.get(providerName, getAdminAccess());
            if (provider.getActive() && provider.getStatus().equals(Provider.States.APPROVED.getKey())) {
                if (provider.getUsers().stream().anyMatch(u -> u.getEmail().equals(user.getEmail()))) {
                    return true;
                }
            } else if (provider.getStatus().equals(Provider.States.PENDING_2.getKey())) {
                FacetFilter ff = new FacetFilter();
                ff.addFilter("providers", provider.getId());
                if (infraServiceService.getAll(ff, getAdminAccess()).getResults().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
