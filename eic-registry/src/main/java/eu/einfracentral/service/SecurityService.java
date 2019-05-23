package eu.einfracentral.service;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.User;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.manager.ProviderManager;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.ServiceException;
import org.mitre.openid.connect.model.DefaultUserInfo;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service("securityService")
public class SecurityService {

    private ProviderManager providerManager;
    private InfraServiceService<InfraService, InfraService> infraServiceService;
    private OIDCAuthenticationToken adminAccess;

    @Autowired
    SecurityService(ProviderManager providerManager, InfraServiceService<InfraService, InfraService> infraServiceService) {
        this.providerManager = providerManager;
        this.infraServiceService = infraServiceService;

        // create admin access
        List<GrantedAuthority> roles = new ArrayList<>();
        roles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        DefaultUserInfo userInfo = new DefaultUserInfo();
        userInfo.setEmail("no-reply@einfracentral.eu");
        userInfo.setId(1L);
        userInfo.setGivenName("eInfraCentral");
        userInfo.setFamilyName("");
        adminAccess = new OIDCAuthenticationToken("", "", userInfo, roles, null, "", "");
    }

    public Authentication getAdminAccess() {
        return adminAccess;
    }

    public boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }

    public boolean userIsProviderAdmin(Authentication auth, Provider provider) {
        return userIsProviderAdmin(auth, provider.getId());
    }

    public boolean userIsProviderAdmin(Authentication auth, String providerId) {
        Provider registeredProvider = providerManager.get(providerId);
        User user = new User(auth);
        if (registeredProvider == null) {
            throw new ServiceException("Provider with id '" + providerId + "' does not exist.");
        }
        return registeredProvider.getUsers()
                .parallelStream()
                .anyMatch(u -> {
                    if (u.getId() != null) {
                        if (u.getEmail() != null) {
                            return u.getId().equals(user.getId())
                                    || u.getEmail().equals(user.getEmail());
                        }
                        return u.getId().equals(user.getId());
                    }
                    return u.getEmail().equals(user.getEmail());
                });
    }

    public boolean userIsServiceProviderAdmin(Authentication auth, eu.einfracentral.domain.Service service) {
        if (service.getProviders().isEmpty()) {
            throw new ValidationException("Service has no providers");
        }
        Optional<List<String>> providers = Optional.of(service.getProviders());
        return providers
                .get()
                .stream()
                .filter(Objects::nonNull)
                .anyMatch(id -> userIsProviderAdmin(auth, id));
    }

    public boolean userIsServiceProviderAdmin(Authentication auth, String serviceId) {
        InfraService service = infraServiceService.get(serviceId);
        if (service.getProviders().isEmpty()) {
            throw new ValidationException("Service has no providers");
        }
        Optional<List<String>> providers = Optional.of(service.getProviders());
        return providers
                .get()
                .stream()
                .filter(Objects::nonNull)
                .anyMatch(id -> userIsProviderAdmin(auth, id));
    }

    public boolean providerCanAddServices(Authentication auth, InfraService service) {
        List<String> providerNames = service.getProviders();
        for (String providerName : providerNames) {
            Provider provider = providerManager.get(providerName);
            if (provider.getActive() && provider.getStatus().equals(Provider.States.APPROVED.getKey())) {
                return userIsProviderAdmin(auth, provider);
            } else if (provider.getStatus().equals(Provider.States.ST_SUBMISSION.getKey())) {
                FacetFilter ff = new FacetFilter();
                ff.addFilter("providers", provider.getId());
                if (infraServiceService.getAll(ff, getAdminAccess()).getResults().isEmpty()) {
                    return true;
                }
                throw new ServiceException("You have already created a Service Template.");
            }
        }
        return false;
    }

    public boolean providerIsActive(String providerId) {
        Provider provider = providerManager.get(providerId);
        if (!provider.getActive()) {
            throw new ServiceException(String.format("Provider with id '%s' is not active yet.", provider.getName()));
        }
        return provider.getActive();
    }

    public boolean providerIsActiveAndUserIsAdmin(Authentication auth, String serviceId) {
        InfraService service = infraServiceService.get(serviceId);
        for (String providerId : service.getProviders()) {
            Provider provider = providerManager.get(providerId);
            if (provider.getActive()) {
                return userIsProviderAdmin(auth, provider);
            }
        }
        return false;
    }
}
