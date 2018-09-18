package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.Service;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.openminted.registry.core.domain.FacetFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

@Component
public class ProviderManager extends ResourceManager<Provider> implements ProviderService<Provider, Authentication> {
    @Autowired
    private InfraServiceService<InfraService, InfraService> infraServiceService;

    public ProviderManager() {
        super(Provider.class);
    }

    @Override
    public String getResourceType() {
        return "provider";
    }

    @Override
    public List<Provider> getMyServiceProviders(String email) {
        return getAll(new FacetFilter(), null).getResults()
                .stream().map(p -> {
                    if (p.getUsers().stream().filter(Objects::nonNull).anyMatch(u -> u.getEmail().equals(email))) {
                        return p;
                    } else return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Service> getServices(String id) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("provider", id);
        ff.setFrom(0);
        ff.setQuantity(1000);
        return infraServiceService.getAll(ff, null).getResults().stream().map(Service::new).collect(Collectors.toList());
    }

    @Override
    public Service getFeaturedService(String id) {
        List<Service> services = getServices(id);
        Service featuredService = null;
        if (services.size() > 0) {
            Random random = new Random();
            featuredService = services.get(random.nextInt(services.size()));
        }
        return featuredService;
    }
}
