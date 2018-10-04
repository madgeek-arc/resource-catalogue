package eu.einfracentral.registry.manager;

import eu.einfracentral.utils.AuthenticationDetails;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.User;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.utils.ObjectUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service("providerManager")
public class ProviderManager extends ResourceManager<Provider> implements ProviderService<Provider, Authentication> {

    private static final Logger logger = LogManager.getLogger(ProviderManager.class);
    private InfraServiceService<InfraService, InfraService> infraServiceService;

    @Autowired
    public ProviderManager(InfraServiceService<InfraService, InfraService> infraServiceService) {
        super(Provider.class);
        this.infraServiceService = infraServiceService;
    }

    @Override
    public String getResourceType() {
        return "provider";
    }

    @Override
    public Provider add(Provider provider, Authentication auth) {
        List<User> users;
        try {
            String email = AuthenticationDetails.getEmail(auth);
            users = provider.getUsers();
            if (users == null) {
                users = new ArrayList<>();
            }
            if (users.stream().noneMatch(user -> user.getEmail().equals(email))) {
                User user = new User();
                user.setEmail(email);
                user.setId(AuthenticationDetails.getSub(auth));
                user.setName(AuthenticationDetails.getGivenName(auth));
                user.setSurname(AuthenticationDetails.getFamilyName(auth));
                users.add(user);
                provider.setUsers(users);
            }
        } catch (Exception e) {
            logger.error(e);
            throw new AuthorizationServiceException("Could not create Provider", e);
        }
        provider.setActive(false);
        provider.setStatus(Provider.States.PENDING_1.getKey());
        return super.add(provider, null);
    }

    @Override
    public Provider update(Provider provider, Authentication auth) {
//        update(provider, auth);
        Resource existing = whereID(provider.getId(), true);
        Provider ex = deserialize(existing);
        ObjectUtils.merge(ex, provider);
        existing.setPayload(serialize(ex));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);

        return ex;
    }

    @Override
    public Provider verifyProvider(String id, Provider.States status, Boolean active, Authentication auth) {
        Provider provider = get(id);
        if (active != null) {
            provider.setActive(active);
        }
        switch (status) {
            case REJECTED:
                logger.info("Deleting provider: " + provider.getName());
                this.delete(provider);
                break;
            case APPROVED:
                provider.setActive(true);
                break;
            case PENDING_1:
                provider.setActive(false);
                break;
            case PENDING_2:
                provider.setActive(false);
                break;
            case REJECTED_ST:
                provider.setActive(false);
                break;
            default:
        }
        provider.setStatus(status.getKey());
        return update(provider, auth);
    }

    @Override
    public List<Provider> getMyServiceProviders(String email) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        return getAll(ff, null).getResults()
                .stream().map(p -> {
                    if (p.getUsers().stream().filter(Objects::nonNull).anyMatch(u -> u.getEmail().equals(email))) {
                        return p;
                    } else return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Service> getServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("providers", providerId);
        ff.setQuantity(10000);
        return infraServiceService.getAll(ff, null).getResults().stream().map(Service::new).collect(Collectors.toList());
    }

    @Override
    public Service getFeaturedService(String providerId) {
        List<Service> services = getServices(providerId);
        Service featuredService = null;
        if (!services.isEmpty()) {
            Random random = new Random();
            featuredService = services.get(random.nextInt(services.size()));
        }
        return featuredService;
    }

    @Override
    public List<Provider> getInactive() {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(10000);
        return getAll(ff, null).getResults();
    }

    @Override
    public List<InfraService> getInactiveServices(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("providers", providerId);
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(10000);
        return infraServiceService.getAll(ff, null).getResults();
    }
}
