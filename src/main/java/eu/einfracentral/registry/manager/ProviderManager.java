package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.*;
import eu.openminted.registry.core.domain.FacetFilter;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by pgl on 26/7/2017.
 */
@org.springframework.stereotype.Service("providerService")
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
    public Map<String, Integer> visits(String id) {
        Map<String, Integer> ret = new HashMap<>();
        getServices(id).stream().forEach(s -> {
            Map<String, Integer> visits = serviceService.visits(s.getId());
            visits.forEach((k, v) -> {
                ret.putIfAbsent(k, 0);
                ret.put(k, ret.get(k) + v);
            });
        });
        return ret;
    }
        return ret;
    }
}
