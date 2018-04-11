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

    public Map<String, Float> visitation(String id) {
        Map<String, Float> ret = new HashMap<>();
        List<Service> services = getServices(id);
        return ret;
    }
    private List<Service> getServices(String id) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("provider", id);
        return serviceService.getAll(ff).getResults();
    }

    @Override
    public Map<String, Integer> favourites(String id) {
        Map<String, Integer> ret = new HashMap<>();
        getServices(id).stream().forEach(s -> {
            Map<String, Integer> favourites = serviceService.favourites(s.getId());
            favourites.forEach((k, v) -> {
                ret.putIfAbsent(k, 0);
                ret.put(k, ret.get(k) + v);
            });
        });
        return ret;
    }

    @Override
    public Map<String, Float> ratings(String id) {
        Map<String, Float> ret = new HashMap<>();
        getServices(id).stream().forEach(s -> {
            Map<String, Float> ratings = serviceService.ratings(s.getId());
            ratings.forEach((k, v) -> {
                ret.putIfAbsent(k, 0f);
                ret.put(k, ret.get(k) + v);
            });
        });
        return ret;
    }
}
