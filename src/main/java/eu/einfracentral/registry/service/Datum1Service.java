package eu.einfracentral.registry.service;


import eu.einfracentral.registry.domain.Page;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by stefanos on 20/6/2017.
 */

@Component
public class Datum1Service implements Datum1Interface {

    private Logger logger = Logger.getLogger(Datum1Service.class);

    @Autowired
    SearchService searchService;

    @Autowired
    ResourceService resourceService;

    /**
     * Type reflection class
     */
    //final private Class<T> typeParameterClass;
    public Datum1Service() {
//        Class<T> typeParameterClass
//        this.typeParameterClass = typeParameterClass;
    }

    public String get(String id) {
        return id;
    }


    public Page<String> getAll(FacetFilter filter) {
        throw new ServiceException("Not yet implemented");
    }

    public Page<String> getMy(FacetFilter filter) {
        throw new ServiceException("Not yet implemented");
    }

    public void add(String resource) {
        throw new ServiceException("Not yet implemented");
        // Resource resourceDb = null;
        // resourceService.addResource(resourceDb);
    }

    public void update(String resources) {
        throw new ServiceException("Not yet implemented");
    }

    public void delete(String component) {
        throw new ServiceException("Not yet implemented");
    }
}
