package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.manager.StatisticsManager;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ServiceInterface;
import eu.einfracentral.utils.ObjectUtils;
import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public abstract class ServiceResourceManager extends AbstractGenericService<InfraService> implements InfraServiceService<InfraService, InfraService>,
        ServiceInterface<InfraService, InfraService, Authentication> {

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

    @Override
    public InfraService get(String id, String version) {
        Resource resource = getResource(id, version);
        return resource != null ? deserialize(resource) : null;
    }

    @Override
    public InfraService getLatest(String id) throws ResourceNotFoundException {
        List<Resource> resources = searchService
                .cqlQuery(String.format("infra_service_id=\"%s\" AND active=true", id), "infra_service",
//                .cqlQuery(String.format("infra_service_id=\"%s\"", id), "infra_service", // TODO: verify that the above works
                        1, 0, "creation_date", "DESC").getResults();
        if (resources.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        return deserialize( resources.get(0));
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
    public InfraService update(InfraService infraService, Authentication auth) {
        Resource existing = getResource(infraService.getId(), infraService.getVersion());
        assert existing != null;
        InfraService ex = deserialize(existing);
        assert ex != null;
        ObjectUtils.merge(ex, infraService);
        existing.setPayload(serialize(ex));
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
            logger.warn("Attempt to find field '" + field + "' in Service failed: ", e);
            serviceField = InfraService.class.getDeclaredField(field);
        }
        serviceField.setAccessible(true);

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
    public List<RichService> getByIds(Authentication auth, String... ids) {
        List<RichService> services;
        services = Arrays.stream(ids).map(id -> {
            try {
                return getLatest(id);
            } catch (ResourceNotFoundException e) {
                logger.error("Could not find InfraService with id: " + id, e);
                throw new ServiceException(e);
            }
        }).map(service -> createRichService(service, auth)).collect(toList());
        return services;
    }

    @Override
    public Browsing<RichService> getRichServices(FacetFilter ff, Authentication auth) {
        Browsing<InfraService> infraServices = getAll(ff, null);
        List<RichService> services = infraServices.getResults()
                .parallelStream()
                .map(service -> createRichService(service, auth))
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
        List<Resource> resource = searchService.cqlQuery(String.format("id = \"%s\"", resourceId), resourceType.getName(),
                1, 0, "registeredAt", "DESC").getResults();
        if (resource.isEmpty()) {
            return null;
        }
        return resource.get(0);
    }

    public Resource getResource(String serviceId, String serviceVersion) {
        Paging<Resource> resources;
        if (serviceVersion == null || "".equals(serviceVersion)) {
            resources = searchService
                    .cqlQuery(String.format("infra_service_id = \"%s\"", serviceId),
                            resourceType.getName(), 1, 0, "registeredAt", "DESC");
        } else {
            resources = searchService
                    .cqlQuery(String.format("infra_service_id = \"%s\" AND service_version = \"%s\"", serviceId, serviceVersion), resourceType.getName());
        }
        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults().get(0);
    }

    private List<Resource> getResourcesWithServiceId(String infraServiceId) {
        Paging<Resource> resources;
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

    @Override
    public RichService createRichService(InfraService infraService, Authentication auth) {
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

        // set favourite
        List<Event> userEvents;
        try {
            userEvents = eventManager.getEvents(Event.UserActionType.FAVOURITE.getKey(), infraService.getId(), auth);
            if (!userEvents.isEmpty()) {
                richService.setFavourite(userEvents.get(0).getValue().equals("1"));
            }
            userEvents = eventManager.getEvents(Event.UserActionType.RATING.getKey(), infraService.getId(), auth);
            if (!userEvents.isEmpty()) {
                richService.setUserRate(Float.parseFloat(userEvents.get(0).getValue()));
            }
        } catch (Exception e) {
            logger.error(e);
        }

        //set Ratings & Favourites
        richService.setRatings(eventManager.getServiceEvents(Event.UserActionType.RATING.getKey(), infraService.getId())
                .stream()
                .map(Event::getUser)
                .distinct()
                .mapToInt(u -> 1)
                .sum());

        Optional<List<Event>> favourites = Optional.ofNullable(eventManager.getServiceEvents(Event.UserActionType.FAVOURITE.getKey(), infraService.getId()));
        Map<String, Integer> userFavourites = new HashMap<>();
        favourites.ifPresent(f -> f
                .stream()
                .filter(x -> x.getValue() != null)
                .forEach(e -> userFavourites.putIfAbsent(e.getUser(), Integer.parseInt(e.getValue()))));
        int favs = 0;
        for (Map.Entry<String, Integer> entry : userFavourites.entrySet()) {
            favs += entry.getValue();
        }
        richService.setFavourites(favs);

        // set rating
        Optional<List<Event>> ratings = Optional.ofNullable(eventManager.getServiceEvents(Event.UserActionType.RATING.getKey(), infraService.getId()));
        Map<String, Float> userRatings = new HashMap<>();
        ratings.ifPresent(r -> r.stream().filter(x -> x.getValue() != null).forEach(rating -> userRatings.putIfAbsent(rating.getUser(), Float.parseFloat(rating.getValue()))));
        float sum = 0;
        for (Map.Entry<String, Float> entry : userRatings.entrySet()) {
            sum += entry.getValue();
        }
        if (!userRatings.isEmpty()) {
            richService.setHasRate(Float.parseFloat(new DecimalFormat("#.##").format(sum/userRatings.size()))); //the rating of the specific service as x.xx (3.33)
        } else {
            richService.setHasRate(0);
        }

        // set visits
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
