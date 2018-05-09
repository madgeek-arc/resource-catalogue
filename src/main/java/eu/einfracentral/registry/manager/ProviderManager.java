package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.*;
import eu.openminted.registry.core.domain.FacetFilter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProviderManager extends ResourceManager<Provider> implements ProviderService {
    @Autowired
    private ServiceService serviceService;

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
        return serviceService.getAll(ff).getResults();
    }
}
