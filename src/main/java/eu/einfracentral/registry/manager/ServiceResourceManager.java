package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.AbstractGenericService;
import eu.openminted.registry.core.service.ParserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ServiceResourceManager extends AbstractGenericService<InfraService> implements InfraServiceService {

    private static final Logger logger = LogManager.getLogger(ServiceResourceManager.class);

    public ServiceResourceManager(Class<InfraService> typeParameterClass) {
        super(typeParameterClass);
    }

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
    public InfraService getLatest(String id) {
        List resources = searchService
                .cqlQuery("infra_service_id=" + id, "infra_service", 1, 0, "registeredAt", "DESC")
                .getResults();
        return resources.isEmpty() ? null : deserialize((Resource) resources.get(0));
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
    public InfraService update(InfraService infraService) {
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

    public Resource getResource(String id, String version) {
        Paging resources = null;
        if (version == null) {
            resources = searchService
                    .cqlQuery(String.format("infra_service_id = %s AND service_version = %s", id, version),
                            resourceType.getName(), 1, 0, "registeredAt", "DESC");
        } else {
            resources = searchService
                    .cqlQuery(String.format("infra_service_id = %s AND service_version = %s", id, version), resourceType.getName());
        }
        assert resources != null;
        return resources.getTotal() == 0 ? null : (Resource) resources.getResults().get(0);
    }

}
