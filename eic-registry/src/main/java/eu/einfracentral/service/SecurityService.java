package eu.einfracentral.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.User;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.manager.ProviderManager;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.ServiceException;
import org.mitre.openid.connect.model.DefaultUserInfo;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.*;

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
            throw new ResourceNotFoundException("Provider with id '" + providerId + "' does not exist.");
        }
        if (registeredProvider.getUsers() == null) {
            return false;
        }
        return registeredProvider.getUsers()
                .parallelStream()
                .filter(Objects::nonNull)
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
        InfraService service;
        try {
            service = infraServiceService.get(serviceId);
        } catch (RuntimeException e) {
            return false;
        }
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

    public boolean userIsServiceProviderAdmin(Authentication auth, Map<String, JsonNode> json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        eu.einfracentral.domain.Service service = null;
        service = mapper.readValue(json.get("service").toString(), eu.einfracentral.domain.Service.class);
        if (service == null) {
            throw new ServiceException("Service is null");
        }

        if (service.getProviders() == null || service.getProviders().isEmpty()) {
            throw new ValidationException("Service has no providers");
        }

        return service.getProviders()
                .stream()
                .filter(Objects::nonNull)
                .anyMatch(id -> userIsProviderAdmin(auth, id));
    }

    public boolean providerCanAddServices(Authentication auth, InfraService service) {
        List<String> providerIds = service.getProviders();
        for (String providerId : providerIds) {
            Provider provider = providerManager.get(providerId);
            if ((provider.getActive() == null || provider.getStatus() == null)) {
                throw new ServiceException("Provider active or status field is null");
            }
            if (provider.getActive() && provider.getStatus().equals(Provider.States.APPROVED.getKey())) {
                if (userIsProviderAdmin(auth, provider)) {
                    return true;
                }
            } else if (provider.getStatus().equals(Provider.States.ST_SUBMISSION.getKey())) {
                FacetFilter ff = new FacetFilter();
                ff.addFilter("providers", provider.getId());
                if (infraServiceService.getAll(ff, getAdminAccess()).getResults().isEmpty()) {
                    return true;
                }
                throw new ResourceException("You have already created a Service Template.", HttpStatus.CONFLICT);
            }
        }
        return false;
    }

    public boolean providerIsActive(String providerId) {
        Provider provider = providerManager.get(providerId);
        if (provider != null && provider.getActive() != null) {
            if (!provider.getActive()) {
                throw new ServiceException(String.format("Provider '%s' is not active.", provider.getName()));
            }
            return true;
        } else {
            throw new ResourceNotFoundException(String.format("Provider with id '%s' does not exist.", providerId));
        }
    }

    public boolean providerIsActiveAndUserIsAdmin(Authentication auth, String serviceId) {
        InfraService service = infraServiceService.get(serviceId);
        for (String providerId : service.getProviders()) {
            Provider provider = providerManager.get(providerId);
            if (provider != null && provider.getActive() != null && provider.getActive()) {
                if (userIsProviderAdmin(auth, providerId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean serviceIsActive(String serviceId) {
        InfraService service = infraServiceService.get(serviceId);
        return service.isActive();
    }

    public boolean serviceIsActive(String serviceId, String version) {
        // FIXME: serviceId is equal to 'rich' and version holds the service ID
        //  when searching for a Rich Service without providing a version
        if ("rich".equals(serviceId)) {
            return serviceIsActive(version);
        }
        InfraService service = infraServiceService.get(serviceId, version);
        return service.isActive();
    }
}
