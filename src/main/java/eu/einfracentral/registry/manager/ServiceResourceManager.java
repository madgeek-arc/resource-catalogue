package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.ServiceHistory;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.AbstractGenericService;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.ServiceException;
import eu.openminted.registry.core.service.VersionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class ServiceResourceManager extends AbstractGenericService<InfraService> implements InfraServiceService {

    private static final Logger logger = LogManager.getLogger(ServiceResourceManager.class);

    public ServiceResourceManager(Class<InfraService> typeParameterClass) {
        super(typeParameterClass);
    }

    @Autowired
    VersionService versionService;

    @Override
    public String getResourceType() {
        return resourceType.getName();
    }

    @Override
    public InfraService get(String id, String version) {
        Resource resource = getResource(id, version);
        return resource != null ? deserialize(resource) : null;
    }

    @Override
    public InfraService getLatest(String id) throws ResourceNotFoundException {
        List resources = searchService
                .cqlQuery("infra_service_id=\"" + id + "\"", "infra_service", 1, 0, "registeredAt", "DESC")
                .getResults();
        if (resources.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        return deserialize((Resource) resources.get(0));
    }

    @Override
    public InfraService get(String id) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public Browsing<InfraService> getAll(FacetFilter filter) {
        filter.setBrowseBy(getBrowseBy());
        return getResults(filter);
    }

    @Override
    public Browsing<InfraService> getMy(FacetFilter filter) {
        throw new UnsupportedOperationException("Not yet Implemented");
    }

    @Override
    public InfraService add(InfraService infraService) {
        if (exists(infraService)) {
            throw new ResourceException(String.format("%s already exists!", resourceType.getName()), HttpStatus.CONFLICT);
        }
        String serialized = null;
        try {
            serialized = parserPool.serialize(infraService, ParserService.ParserServiceTypes.XML).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error(e);
        }
        Resource created = new Resource();
        created.setPayload(serialized);
        created.setResourceType(resourceType);
        resourceService.addResource(created);
        return infraService;
    }

    @Override
    public InfraService update(InfraService infraService) throws ResourceNotFoundException {
        String serialized = null;
        Resource existing = null;
        try {
            serialized = parserPool.serialize(infraService, ParserService.ParserServiceTypes.XML).get();
            existing = getResource(infraService.getId(), infraService.getVersion());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        assert existing != null;
        existing.setPayload(serialized);
        resourceService.updateResource(existing);
        return infraService;
    }

    @Override
    public void delete(InfraService infraService) {
        resourceService.deleteResource(getResource(infraService.getId(), infraService.getVersion()).getId());
    }

    @Override
    public Map<String, List<InfraService>> getBy(String field) throws NoSuchFieldException {
        Field serviceField = null;
        try {
            serviceField = Service.class.getDeclaredField(field);
        } catch (NoSuchFieldException e) {
            logger.warn("Attempt to find field " + field + " in Service failed: ", e);
            serviceField = InfraService.class.getDeclaredField(field);
        } finally {
            assert serviceField != null;
            serviceField.setAccessible(true);
        }

        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        Browsing<InfraService> services = getAll(ff);

        final Field f = serviceField;
        return services.getResults().stream()/*.map(Service::new)*/.collect(Collectors.groupingBy(service -> {
            try {
                return f.get(service) != null ? f.get(service).toString() : "undefined";
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                try {
                    return f.get(service) != null ? f.get(service).toString() : "undefined";
                } catch (IllegalAccessException e1) {
                    e1.printStackTrace();
                }
                return "undefined";
            }
        }, Collectors.mapping((InfraService service) -> service, toList())));
    }

    @Override
    public List<InfraService> getByIds(String... ids) {
        List<InfraService> services;
        services = Arrays.stream(ids).map(id -> {
            try {
                return getLatest(id);
            } catch (ResourceNotFoundException e) {
                logger.error("Could not find InfraService with id: "+ id, e);
                throw new ServiceException(e);
            }
        }).collect(toList());
        return services;
    }

    @Override
    public Browsing<ServiceHistory> getHistory(String service_id) {
        List<ServiceHistory> history = new ArrayList<>();

        // get all resources with the specified Service id
        List<Resource> resources = getResourcesWithServiceId(service_id);

        // for each resource (InfraService), get its versions
        for (Resource resource : resources) {
            List<Version> versions = versionService.getVersionsByResource(resource.getId());
            if (versions.size() == 0) { // if there are no versions, keep the service resource (fix for when getting 0 versions)
                InfraService service = deserialize(resource);
                history.add(new ServiceHistory(service.getServiceMetadata(), service.getVersion()));
            } else {
                for (Version version : versions) {
                    Resource tempResource = version.getResource();
                    tempResource.setPayload(version.getPayload());
                    InfraService service = deserialize(tempResource);
                    history.add(new ServiceHistory(service.getServiceMetadata(), service.getVersion()));
                }
                history.get(history.size()-1).setVersionChange(true);
            }
        }

        return new Browsing<>(history.size(), 0, history.size(), history, null);
    }

    public String serialize(InfraService infraService) {
        String serialized;
        try {
            serialized = parserPool.serialize(infraService, ParserService.ParserServiceTypes.XML).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error(e);
            throw new ResourceException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return serialized;
    }

    public InfraService deserialize(Resource resource) {
        if (resource == null) {
            logger.warn("attempt to deserialize null resource");
            return null;
        }
        try {
            return parserPool.deserialize(resource, InfraService.class).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error(e);
            throw new ResourceException(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public boolean exists(InfraService infraService) {
        return getResource(infraService.getId(), infraService.getVersion()) != null;
    }

    public Resource getResourceById(String resourceId) {
        List resource = searchService.cqlQuery(String.format("id = \"%s\"", resourceId), resourceType.getName(),
                1, 0, "registeredAt", "DESC").getResults();
        if (resource.isEmpty()) {
            return null;
        }
        return (Resource) resource.get(0);
    }

    public Resource getResource(String serviceId, String serviceVersion) {
        Paging resources = null;
        if (serviceVersion == null) {
            resources = searchService
                    .cqlQuery(String.format("infra_service_id = \"%s\"", serviceId),
                            resourceType.getName(), 1, 0, "registeredAt", "DESC");
        } else {
            resources = searchService
                    .cqlQuery(String.format("infra_service_id = \"%s\" AND service_version = \"%s\"", serviceId, serviceVersion), resourceType.getName());
        }
        assert resources != null;
        return resources.getTotal() == 0 ? null : (Resource) resources.getResults().get(0);
    }

    public List<Resource> getResourcesWithServiceId(String infraServiceId) {
        Paging resources = null;
            resources = searchService
                    .cqlQuery(String.format("infra_service_id = \"%s\"", infraServiceId),
                            resourceType.getName(), 10000, 0, "registeredAt", "DESC");

        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults();
    }

    protected Map<String, List<Resource>> groupBy(String field) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceType.getName());
        ff.setQuantity(1000);
        Map<String, List<Resource>> res = searchService.searchByCategory(ff, field);
        return res;
    }
}
