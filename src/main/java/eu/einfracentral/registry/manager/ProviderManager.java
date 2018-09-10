package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.*;
import eu.openminted.registry.core.domain.FacetFilter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProviderManager extends ResourceManager<Provider> implements ProviderService {
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
    public List<Service> getServices(String id) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("provider", id);
        ff.setFrom(0);
        ff.setQuantity(1000);
        return infraServiceService.getAll(ff, null).getResults().stream().map(Service::new).collect(Collectors.toList());
    }
}
