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

import javax.validation.constraints.NotNull;
import java.util.*;

@Service("securityService")
public class OIDCSecurityService implements SecurityService {

    private final ProviderManager providerManager;
    private final InfraServiceService<InfraService, InfraService> infraServiceService;
    private final PendingResourceService<InfraService> pendingServiceManager;
    private OIDCAuthenticationToken adminAccess;

    @Value("${project.name:}")
    private String projectName;

    @Value("${mail.smtp.user:}")
    private String projectEmail;

    @Autowired
    OIDCSecurityService(ProviderManager providerManager,
                    InfraServiceService<InfraService, InfraService> infraServiceService,
                    PendingResourceService<InfraService> pendingServiceManager) {
        this.providerManager = providerManager;
        this.infraServiceService = infraServiceService;
        this.pendingServiceManager = pendingServiceManager;

        // create admin access
        List<GrantedAuthority> roles = new ArrayList<>();
        roles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        DefaultUserInfo userInfo = new DefaultUserInfo();
        userInfo.setEmail(projectEmail);
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

    public boolean isProviderAdmin(Authentication auth, @NotNull String providerId) {
        User user = User.of(auth);
        return userIsProviderAdmin(user, providerId);
    }

    public boolean userIsProviderAdmin(User user, @NotNull String providerId) {
        ProviderBundle registeredProvider = providerManager.get(providerId);
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

        User user = User.of(auth);
        if (service.getResourceOrganisation() == null || service.getResourceOrganisation().equals("")) {
            throw new ValidationException("Service has no Resource Organisation");
        }
//        List<String> allProviders = service.getResourceProviders();
//        allProviders.add(service.getResourceOrganisation());
        List<String> allProviders = Collections.singletonList(service.getResourceOrganisation());
        Optional<List<String>> providers = Optional.of(allProviders);
        return providers
                .get()
                .stream()
                .filter(Objects::nonNull)
                .anyMatch(id -> userIsProviderAdmin(user, id));
    }

    public boolean userIsServiceProviderAdmin(Authentication auth, eu.einfracentral.domain.Service service) {
        if (service.getResourceOrganisation() == null || service.getResourceOrganisation().equals("")) {
            throw new ValidationException("Service has no Service Organisation");
        }
        User user = User.of(auth);
//        List<String> allProviders = service.getResourceProviders();
//        allProviders.add(service.getResourceOrganisation());
        List<String> allProviders = Collections.singletonList(service.getResourceOrganisation());
        Optional<List<String>> providers = Optional.of(allProviders);
        return providers
                .get()
                .stream()
                .filter(Objects::nonNull)
                .anyMatch(id -> userIsProviderAdmin(user, id));
    }

    public boolean userIsServiceProviderAdmin(Authentication auth, InfraService infraService) {
        return userIsServiceProviderAdmin(auth, infraService.getService());
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
        if (service.getService().getResourceOrganisation() == null || service.getService().getResourceOrganisation().equals("")) {
            throw new ValidationException("Service has no Service Organisation");
        }
        User user = User.of(auth);
//        List<String> allProviders = service.getService().getResourceProviders();
//        allProviders.add(service.getService().getResourceOrganisation());
        List<String> allProviders = Collections.singletonList(service.getService().getResourceOrganisation());
        Optional<List<String>> providers = Optional.of(allProviders);
        return providers
                .get()
                .stream()
                .filter(Objects::nonNull)
                .anyMatch(id -> userIsProviderAdmin(user, id));
    }

    public boolean providerCanAddServices(Authentication auth, String serviceId) {
        return providerCanAddServices(auth, infraServiceService.get(serviceId));
    }

    public boolean providerCanAddServices(Authentication auth, InfraService infraService) {
        return providerCanAddServices(auth, infraService.getService());
    }

    public boolean providerCanAddServices(Authentication auth, eu.einfracentral.domain.Service service) {
//        List<String> providerIds = service.getService().getResourceProviders();
//        providerIds.add((service.getService().getResourceOrganisation()));
        List<String> providerIds = Collections.singletonList(service.getResourceOrganisation());
        for (String providerId : providerIds) {
            ProviderBundle provider = providerManager.get(providerId);
            if (isProviderAdmin(auth, provider.getId())) {
                if (provider.getStatus() == null) {
                    throw new ServiceException("Provider status field is null");
                }
                if (provider.isActive() && provider.getStatus().equals(Provider.States.APPROVED.getKey())) {
                    if (isProviderAdmin(auth, provider.getId())) {
                        return true;
                    }
                } else if (provider.getStatus().equals(Provider.States.ST_SUBMISSION.getKey())) {
                    FacetFilter ff = new FacetFilter();
                    ff.addFilter("resource_organisation", provider.getId());
                    if (infraServiceService.getAll(ff, getAdminAccess()).getResults().isEmpty()) {
                        return true;
                    }
                    throw new ResourceException("You have already created a Service Template.", HttpStatus.CONFLICT);
                }
            }
        }
        return false;
    }

    public boolean providerCanAddServices(Authentication auth, Map<String, JsonNode> json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        eu.einfracentral.domain.Service service = null;
        service = mapper.readValue(json.get("service").toString(), eu.einfracentral.domain.Service.class);
        if (service == null) {
            throw new ServiceException("Service is null");
        }
        if (service.getResourceOrganisation() == null || service.getResourceOrganisation().equals("")) {
            throw new ValidationException("Service has no Service Organisation");
        }

        return providerCanAddServices(auth, service);
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
//        List<String> providerIds = service.getService().getResourceProviders();
//        providerIds.add(service.getService().getResourceOrganisation());
        List<String> providerIds = Collections.singletonList(service.getService().getResourceOrganisation());
        for (String providerId : providerIds) {
            ProviderBundle provider = providerManager.get(providerId);
            if (provider != null && provider.isActive()) {
                if (isProviderAdmin(auth, providerId)) {
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
