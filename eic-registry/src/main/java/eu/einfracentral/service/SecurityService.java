package eu.einfracentral.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.domain.User;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.manager.ProviderManager;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.ServiceException;
import org.mitre.openid.connect.model.DefaultUserInfo;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("securityService")
public class SecurityService {

    private final ProviderManager providerManager;
    private final InfraServiceService<InfraService, InfraService> infraServiceService;
    private final PendingResourceService<InfraService> pendingServiceManager;
    private OIDCAuthenticationToken adminAccess;

    @Value("${project.name:}")
    private String projectName;

    @Autowired
    SecurityService(ProviderManager providerManager,
                    InfraServiceService<InfraService, InfraService> infraServiceService,
                    PendingResourceService<InfraService> pendingServiceManager) {
        this.providerManager = providerManager;
        this.infraServiceService = infraServiceService;
        this.pendingServiceManager = pendingServiceManager;

        // create admin access
        List<GrantedAuthority> roles = new ArrayList<>();
        roles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        DefaultUserInfo userInfo = new DefaultUserInfo();
        userInfo.setEmail("no-reply@einfracentral.eu");
        userInfo.setId(1L);
        userInfo.setGivenName(projectName);
        userInfo.setFamilyName("");
        adminAccess = new OIDCAuthenticationToken("", "", userInfo, roles, null, "", "");
    }

    public Authentication getAdminAccess() {
        return adminAccess;
    }

    public boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }

    public boolean userIsProviderAdmin(Authentication auth, String providerId) {
        ProviderBundle registeredProvider = providerManager.get(providerId);
        User user = new User(auth);
        if (registeredProvider == null) {
            throw new ResourceNotFoundException("Provider with id '" + providerId + "' does not exist.");
        }
        if (registeredProvider.getProvider().getUsers() == null) {
            return false;
        }
        return registeredProvider.getProvider().getUsers()
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

    public boolean userIsServiceProviderAdmin(Authentication auth, Map<String, JsonNode> json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        eu.einfracentral.domain.Service service = null;
        service = mapper.readValue(json.get("service").toString(), eu.einfracentral.domain.Service.class);
        if (service == null) {
            throw new ServiceException("Service is null");
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
        } catch (ResourceException e) {
            try {
                service = pendingServiceManager.get(serviceId);
            } catch (RuntimeException re) {
                return false;
            }
        } catch (RuntimeException e) {
            return false;
        }
        if (service.getService().getProviders().isEmpty()) {
            throw new ValidationException("Service has no providers");
        }
        Optional<List<String>> providers = Optional.of(service.getService().getProviders());
        return providers
                .get()
                .stream()
                .filter(Objects::nonNull)
                .anyMatch(id -> userIsProviderAdmin(auth, id));
    }

    public boolean providerCanAddServices(Authentication auth, InfraService service) {
        List<String> providerIds = service.getService().getProviders();
        for (String providerId : providerIds) {
            ProviderBundle provider = providerManager.get(providerId);
            if (userIsProviderAdmin(auth, provider.getId())) {
                if (provider.getStatus() == null) {
                    throw new ServiceException("Provider status field is null");
                }
                if (provider.isActive() && provider.getStatus().equals(Provider.States.APPROVED.getKey())) {
                    if (userIsProviderAdmin(auth, provider.getId())) {
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
        }
        return false;
    }

    @Deprecated
    public boolean providerIsActive(String providerId) {
        ProviderBundle provider = providerManager.get(providerId);
        if (provider != null) {
            if (!provider.isActive()) {
                throw new ServiceException(String.format("Provider '%s' is not active.", provider.getProvider().getName()));
            }
            return true;
        } else {
            throw new ResourceNotFoundException(String.format("Provider with id '%s' does not exist.", providerId));
        }
    }

    public boolean providerIsActiveAndUserIsAdmin(Authentication auth, String serviceId) {
        InfraService service = infraServiceService.get(serviceId);
        for (String providerId : service.getService().getProviders()) {
            ProviderBundle provider = providerManager.get(providerId);
            if (provider != null && provider.isActive()) {
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
