package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;

import java.util.*;

// TODO: REFACTOR
//  1) Replace user.stream().filter/search? with facet filter  email=x
//  2) Ensure already saved emails (and the emails that will be saved from now on) are in lowercase, and perform search with email in lowercase
//  3) Use facet filter to set status/published/active
//  4) userIsResourceProviderAdmin() -> getMyProviders and then search for w/e
@Service("securityService")
public class OIDCSecurityService implements SecurityService {

    private final CatalogueService catalogueService;
    private final ProviderService providerService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final DraftResourceService<ProviderBundle> draftProviderService;
    private final DraftResourceService<ServiceBundle> draftServiceService;
    private final DraftResourceService<TrainingResourceBundle> draftTrainingResourceService;
    private final DraftResourceService<InteroperabilityRecordBundle> draftInteroperabilityRecordService;
    private final Authentication adminAccess = new AdminAuthentication();

    @Value("${pid.providers.prefix}")
    private String providersPrefix;
    @Value("${pid.services.prefix}")
    private String servicesPrefix;
    @Value("${pid.trainings.prefix}")
    private String trainingsPrefix;
    @Value("${pid.interoperability-frameworks.prefix}")
    private String guidelinesPrefix;
    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    public OIDCSecurityService(@Lazy CatalogueService catalogueService,
                               @Lazy ProviderService providerService,
                               @Lazy ServiceBundleService<ServiceBundle> serviceBundleService,
                               @Lazy TrainingResourceService trainingResourceService,
                               @Lazy InteroperabilityRecordService interoperabilityRecordService,
                               @Lazy DraftResourceService<ProviderBundle> draftProviderService,
                               @Lazy DraftResourceService<ServiceBundle> draftServiceService,
                               @Lazy DraftResourceService<TrainingResourceBundle> draftTrainingResourceService,
                               @Lazy DraftResourceService<InteroperabilityRecordBundle> draftInteroperabilityRecordService) {
        this.catalogueService = catalogueService;
        this.providerService = providerService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.draftProviderService = draftProviderService;
        this.draftServiceService = draftServiceService;
        this.draftTrainingResourceService = draftTrainingResourceService;
        this.draftInteroperabilityRecordService = draftInteroperabilityRecordService;
    }

    public Authentication getAdminAccess() {
        return adminAccess;
    }

    @Override
    public String getRoleName(Authentication authentication) {
        String role;
        if (hasRole(authentication, "ROLE_ADMIN")) {
            role = "admin";
        } else if (hasRole(authentication, "ROLE_EPOT")) {
            role = "EPOT";
        } else if (hasRole(authentication, "ROLE_PROVIDER")) {
            role = "provider";
        } else {
            role = "user";
        }
        return role;
    }

    @Override
    public boolean hasRole(Authentication auth, String role) {
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }


    // region Catalogues & Providers
    @Override
    public boolean isProviderAdmin(Authentication auth, @NotNull String id) {
        return getAuthenticatedUser(auth)
                .map(user -> userIsProviderAdmin(user, id))
                .orElse(false);
    }

    @Override
    public boolean isProviderAdmin(Authentication auth, @NotNull String id, boolean noThrow) {
        if (noThrow) {
            return true;
        }
        return getAuthenticatedUser(auth)
                .map(user -> userIsProviderAdmin(user, id))
                .orElse(false);
    }

    public boolean userIsProviderAdmin(User user, @NotNull String id) {
        boolean isProvider = isProvider(id);
        List<User> users = isProvider ? getProviderUsers(id) : getCatalogueUsers(id);
        if (users == null) {
            return false;
        }
        return users.parallelStream()
                .filter(Objects::nonNull)
                .anyMatch(u -> userMatches(u, user));
    }

    private Optional<User> getAuthenticatedUser(Authentication auth) {
        if (hasRole(auth, "ROLE_ANONYMOUS")) {
            return Optional.empty();
        }
        return Optional.of(User.of(auth));
    }

    private boolean isProvider(String id) {
        return id.startsWith(providersPrefix);
    }

    private List<User> getProviderUsers(String id) {
        ProviderBundle registeredProvider = checkProviderExistence(id);
        if (registeredProvider == null) {
            registeredProvider = checkDraftProviderExistence(id);
        }
        if (registeredProvider == null || registeredProvider.getProvider().getUsers() == null) {
            return null;
        }
        return registeredProvider.getProvider().getUsers();
    }

    private List<User> getCatalogueUsers(String id) {
        CatalogueBundle registeredCatalogue = checkCatalogueExistence(id);
        if (registeredCatalogue == null || registeredCatalogue.getCatalogue().getUsers() == null) {
            return null;
        }
        return registeredCatalogue.getCatalogue().getUsers();
    }

    private ProviderBundle checkProviderExistence(String providerId) {
        try {
            return providerService.get(providerId, adminAccess);
        } catch (ResourceException | ResourceNotFoundException e) {
            return null;
        }
    }

    private ProviderBundle checkDraftProviderExistence(String providerId) {
        try {
            return draftProviderService.get(providerId);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private CatalogueBundle checkCatalogueExistence(String id) {
        try {
            return catalogueService.get(id, adminAccess);
        } catch (ResourceException | ResourceNotFoundException e) {
            return null;
        }
    }

    private boolean userMatches(User u1, User u2) {
        return u1.getEmail().equalsIgnoreCase(u2.getEmail());
    }
    //endregion


    // region Resources
    @Override
    public boolean isResourceProviderAdmin(Authentication auth, String resourceId) {
        return getAuthenticatedUser(auth)
                .map(user -> userIsResourceProviderAdmin(user, resourceId))
                .orElse(false);
    }

    @Override
    public boolean userIsResourceProviderAdmin(@NotNull User user, String resourceId) {
        String providerId = getProviderId(resourceId);
        return userIsProviderAdmin(user, providerId);
    }

    private String getProviderId(String resourceId) {
        String providerId;
        Bundle<?> bundle = determineResourceType(resourceId);
        if (bundle instanceof ServiceBundle) {
            providerId = ((ServiceBundle) bundle).getService().getResourceOrganisation();
        } else if (bundle instanceof TrainingResourceBundle) {
            providerId = ((TrainingResourceBundle) bundle).getTrainingResource().getResourceOrganisation();
        } else {
            providerId = ((InteroperabilityRecordBundle) bundle).getInteroperabilityRecord().getProviderId();
        }
        return providerId;
    }

    private Bundle<?> determineResourceType(String resourceId) {
        if (isService(resourceId)) {
            ServiceBundle serviceBundle = serviceBundleService.getOrElseReturnNull(resourceId);
            if (serviceBundle == null) {
                serviceBundle = draftServiceService.get(resourceId);
            }
            return serviceBundle;
        } else if (isTrainingResource(resourceId)) {
            TrainingResourceBundle trainingResourceBundle = trainingResourceService.getOrElseReturnNull(resourceId);
            if (trainingResourceBundle == null) {
                trainingResourceBundle = draftTrainingResourceService.get(resourceId);
            }
            return trainingResourceBundle;
        } else {
            InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.getOrElseReturnNull(resourceId);
            if (interoperabilityRecordBundle == null) {
                interoperabilityRecordBundle = draftInteroperabilityRecordService.get(resourceId);
            }
            return interoperabilityRecordBundle;
        }
    }

    private boolean isService(String id) {
        return id.startsWith(servicesPrefix);
    }

    private boolean isTrainingResource(String id) {
        return id.startsWith(trainingsPrefix);
    }

    public boolean providerCanAddResources(Authentication auth, gr.uoa.di.madgik.resourcecatalogue.domain.Service service) {
        String providerId = service.getResourceOrganisation();
        ProviderBundle provider = providerService.get(providerId, auth);
        return providerCanAddResources(auth, provider, service.getId());
    }

    public boolean providerCanAddResources(Authentication auth, TrainingResource trainingResource) {
        String providerId = trainingResource.getResourceOrganisation();
        ProviderBundle provider = providerService.get(providerId, auth);
        return providerCanAddResources(auth, provider, trainingResource.getId());
    }

    public boolean providerCanAddResources(Authentication auth, InteroperabilityRecord interoperabilityRecord) {
        String providerId = interoperabilityRecord.getProviderId();
        ProviderBundle provider = providerService.get(providerId, auth);
        return provider.isActive() && provider.getStatus().equals("approved provider"); //TODO: replace with providerIsActiveAndUserIsAdmin
    }

    private boolean providerCanAddResources(Authentication auth, ProviderBundle provider, String resourceId) {
        if (isProviderAdmin(auth, provider.getId())) {
            if (provider.getStatus() == null) {
                throw new ServiceException("Provider status field is null");
            }
            if (provider.isActive() && provider.getStatus().equals("approved provider")) {
                return true;
            } else if (provider.getTemplateStatus().equals("no template status")) {
                return checkIfProviderHasRegisteredAServiceTemplate(provider, resourceId);
            }
        }
        return false;
    }

    private boolean checkIfProviderHasRegisteredAServiceTemplate(ProviderBundle provider, String resourceId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", provider.getId());
        Bundle<?> bundle = determineResourceType(resourceId);
        if (bundle instanceof ServiceBundle) {
            if (serviceBundleService.getAll(ff, getAdminAccess()).getResults().isEmpty()) {
                return true;
            }
        } else if (bundle instanceof TrainingResourceBundle) {
            if (trainingResourceService.getAll(ff, getAdminAccess()).getResults().isEmpty()) {
                return true;
            }
        }
        throw new ResourceException("You have already created a Service Template.", HttpStatus.CONFLICT);
    }

    @Override
    public boolean providerIsActiveAndUserIsAdmin(Authentication auth, String resourceId) {
        String providerId = getProviderId(resourceId);
        ProviderBundle provider = providerService.get(providerId, auth);
        if (provider != null && provider.isActive()) {
            if (isProviderAdmin(auth, providerId)) {
                return true;
            }
        }
        return false;
    }

    public boolean serviceIsActive(String resourceId) {
        ServiceBundle serviceBundle = serviceBundleService.get(resourceId);
        return serviceBundle.isActive();
    }

    public boolean trainingResourceIsActive(String resourceId) {
        TrainingResourceBundle trainingResourceBundle = trainingResourceService.get(resourceId);
        return trainingResourceBundle.isActive();
    }

    public boolean guidelineIsActive(String resourceId) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.get(resourceId);
        return interoperabilityRecordBundle.isActive();
    }
    //endregion
}
