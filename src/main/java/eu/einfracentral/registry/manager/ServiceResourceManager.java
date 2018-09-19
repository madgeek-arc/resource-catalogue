package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.manager.StatisticsManager;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.KeyValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class ServiceResourceManager extends AbstractGenericService<InfraService> implements InfraServiceService<InfraService, InfraService> {

    private static final Logger logger = LogManager.getLogger(ServiceResourceManager.class);
    public ServiceResourceManager(Class<InfraService> typeParameterClass) {
        super(typeParameterClass);
    }

    @Autowired
    private VersionService versionService;

    @Autowired
    private VocabularyManager vocabularyManager;

    @Autowired
    private EventManager eventManager;

    @Autowired
    private StatisticsManager statisticsService;

    @Override
    public String getResourceType() {
        return resourceType.getName();
    }

    //    @Override
    public InfraService addService(InfraService infraService, Authentication auth) throws Exception {
        if (exists(infraService)) {
            throw new ResourceException(String.format("%s already exists!", resourceType.getName()), HttpStatus.CONFLICT);
        }
        String serialized = null;
        serialized = parserPool.serialize(infraService, ParserService.ParserServiceTypes.XML);
        Resource created = new Resource();
        created.setPayload(serialized);
        created.setResourceType(resourceType);
        resourceService.addResource(created);
        return infraService;
    }

    //    @Override
    public InfraService updateService(InfraService infraService, Authentication auth) throws Exception {
        String serialized = null;
        Resource existing = null;
        serialized = parserPool.serialize(infraService, ParserService.ParserServiceTypes.XML);
        existing = getResource(infraService.getId(), infraService.getVersion());
        assert existing != null;
        existing.setPayload(serialized);
        resourceService.updateResource(existing);
        return infraService;
    }

    @Override
    public InfraService get(String id, String version) {
        Resource resource = getResource(id, version);
        return resource != null ? deserialize(resource) : null;
    }

    @Override
    public InfraService getLatest(String id) throws ResourceNotFoundException {
        List resources = searchService
                .cqlQuery("infra_service_id=\"" + id + "\"", "infra_service", 1, 0, "creation_date", "DESC")
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
    public Browsing<InfraService> getAll(FacetFilter filter, Authentication auth) {
        filter.setBrowseBy(getBrowseBy());
        return getResults(filter);
    }

    @Override
    public Browsing<InfraService> getMy(FacetFilter filter, Authentication auth) {
        throw new UnsupportedOperationException("Not yet Implemented");
    }

    @Override
    public InfraService add(InfraService infraService, Authentication auth) {
        if (exists(infraService)) {
            throw new ResourceException(String.format("%s already exists!", resourceType.getName()), HttpStatus.CONFLICT);
        }
        String serialized;
        serialized = parserPool.serialize(infraService, ParserService.ParserServiceTypes.XML);
        Resource created = new Resource();
        created.setPayload(serialized);
        created.setResourceType(resourceType);
        resourceService.addResource(created);
        return infraService;
    }

    @Override
    public InfraService update(InfraService infraService, Authentication auth) throws ResourceNotFoundException {
        String serialized;
        Resource existing;
        serialized = parserPool.serialize(infraService, ParserService.ParserServiceTypes.XML);
        existing = getResource(infraService.getId(), infraService.getVersion());
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
        Browsing<InfraService> services = getAll(ff, null);

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
                logger.error("Could not find InfraService with id: " + id, e);
                throw new ServiceException(e);
            }
        }).collect(toList());
        return services;
    }

    @Override
    public Browsing<RichService> getRichServices(FacetFilter ff) {
        Browsing<InfraService> infraServices = getAll(ff, null);
        List<RichService> services = infraServices.getResults()
                .stream()
                .map(this::FillTransientFields)
                .collect(toList());
        return new Browsing<>(infraServices.getTotal(), infraServices.getFrom(),
                infraServices.getTo(), services, infraServices.getFacets());
    }

    @Override
    public boolean exists(SearchService.KeyValue... ids) {
        Resource resource;
        try {
            resource = this.searchService.searchId(getResourceType(), ids);
            return resource != null;
        } catch (UnknownHostException e) {
            logger.error(e);
            throw new ServiceException(e);
        }
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
                history.get(history.size() - 1).setVersionChange(true);
            }
        }

        return new Browsing<>(history.size(), 0, history.size(), history, null);
    }

    public String serialize(InfraService infraService) {
        String serialized;
        serialized = parserPool.serialize(infraService, ParserService.ParserServiceTypes.XML);
        return serialized;
    }

    private InfraService deserialize(Resource resource) {
        if (resource == null) {
            logger.warn("attempt to deserialize null resource");
            return null;
        }
        return parserPool.deserialize(resource, InfraService.class);
    }

    private boolean exists(InfraService infraService) {
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
        Paging resources;
        if (serviceVersion == null || "".equals(serviceVersion)) {
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

    private List<Resource> getResourcesWithServiceId(String infraServiceId) {
        Paging resources;
        resources = searchService
                .cqlQuery(String.format("infra_service_id = \"%s\"", infraServiceId),
                        resourceType.getName(), 10000, 0, "registeredAt", "DESC");

        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults();
    }

    @Deprecated
    protected Map<String, List<Resource>> groupBy(String field) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceType.getName());
        ff.setQuantity(1000);
        Map<String, List<Resource>> res = searchService.searchByCategory(ff, field);
        return res;
    }


    private RichService FillTransientFields(InfraService infraService){
        //FIXME: vocabularyManager.get() is very slow
        RichService richService = new RichService(infraService);

        //setCategoryName & setSubcategoryName
        if (infraService.getCategory() == null) {
            richService.setCategoryName("null");
        } else {
            richService.setCategoryName(vocabularyManager.get("vocabulary_id", infraService.getCategory()).getName());
        }

        if (infraService.getSubcategory() == null) {
            richService.setSubCategoryName("null");
        } else {
            richService.setSubCategoryName(vocabularyManager.get("vocabulary_id", infraService.getSubcategory()).getName());
        }

        //setLanguageNames
        List<String> languageNames = new ArrayList<>();
        if (infraService.getLanguages() != null) {
            for (String lang : infraService.getLanguages()) {
                Vocabulary language = vocabularyManager.get("vocabulary_id", lang);
                if (language != null) {
                    languageNames.add(language.getName());
                }
            }
            richService.setLanguageNames(languageNames);
        }

        //setFavourites & setFavourite
        Paging<Resource> eventResources = searchService.cqlQuery(String.format("value=1 and type=%s and service=%s",
                Event.UserActionType.FAVOURITE.getKey(), infraService.getId()), "event");
        richService.setFavourites(eventResources.getResults().size()); //0 unrated, 1 rated

        for (Resource event : eventResources.getResults()) {
            Event fav = parserPool.deserialize(event, Event.class);
            String x1 = fav.getUser(); //getUser of event fav is string, so we save it on a new string - x1
            // TODO finish this
//            eventManager.getEvents(Event.UserActionType.FAVOURITE.getKey(), infraService.getId(), authentication);
            richService.setFavourite(false);
        }

        //setRatings & setHasRate
        eventResources = searchService.cqlQuery(String.format("value>0 and type=%s and service=%s",
                Event.UserActionType.RATING.getKey(), infraService.getId()), "event");
        richService.setRatings(eventResources.getResults().size()); //how many ratings the specific service has
        float sum = 0;
        for (Resource event : eventResources.getResults()) {
            Event rat = parserPool.deserialize(event, Event.class);
            String x2 = rat.getValue(); //getValue of event rat is string, so we save it on a new string - x2
            sum += Float.parseFloat(x2); //we need the sum to be float, so we parseFloat(x2)
            if (sum == 0) {
                richService.setHasRate(0);
            } else {
                float average = sum / richService.getRatings();
                richService.setHasRate(Float.parseFloat(new DecimalFormat("#.##").format(average))); //the rating of the specific service as x.xx (3.33)
            }
        }

        Map<String, Integer> visits = statisticsService.visits(infraService.getId());
        List<Integer> visitsList = new ArrayList<>(visits.values());
        int visitSum = 0;
        for (int i : visitsList) {
            visitSum += i;
        }
        richService.setViews(visitSum);

        return richService;
    }
}
