package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Service;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ServiceException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pgl on 4/7/2017.
 */
@org.springframework.stereotype.Service("serviceService")

public class ServiceServiceImpl<T> extends BaseGenericResourceCRUDService<Service> implements ServiceService {

    public ServiceServiceImpl() {
        super(Service.class);
    }

    @Override
    public String getResourceType() {
        return "service";
    }

    @Override
    public String uploadService(String filename, InputStream inputStream) {
        return null;
    }

    @Override
    public Service[] getSome(String... ids) {
        Service[] ret = new Service[ids.length];
        for (int i = 0; i < ret.length; i++) {
            try {
                ret[i] = this.get(ids[i]);
            } catch (ServiceException se) {
                ret[i] = new Service();
                ret[i].setId(ids[i]);
            }
        }
        return ret;
    }

    @Override
    public Map<String, List<Service>> getBy(String field) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(getResourceType());
        Map<String, List<Resource>> results = searchService.searchByCategory(ff, field);
        Map<String, List<Service>> ret = new HashMap<>();
        results.forEach((category, resources) -> {
            List<Service> services = new ArrayList<>();
            for (Resource r : resources) {
                try {
                    services.add(parserPool.serialize(r, Service.class).get());
                } catch (Exception e) {
                    throw new ServiceException(e);
                }
            }
            ret.put(category, services);
        });
        return ret;
    }
}
