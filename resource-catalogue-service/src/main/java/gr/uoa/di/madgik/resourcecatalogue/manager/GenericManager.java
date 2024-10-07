package gr.uoa.di.madgik.resourcecatalogue.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.service.*;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.DefaultFacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.LoggingUtils;
import gr.uoa.di.madgik.resourcecatalogue.utils.ReflectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class GenericManager implements GenericResourceService {
    private static final Logger logger = LoggerFactory.getLogger(GenericManager.class);
    public final SearchService searchService;
    public final ResourceService resourceService;
    public final ResourceTypeService resourceTypeService;
    public final ParserService parserPool;
    public final ObjectMapper objectMapper = new ObjectMapper();
    protected final FacetLabelService facetLabelService;
    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;
    private Map<String, List<String>> browseByMap;
    private Map<String, Map<String, String>> labelsMap;

    protected GenericManager(SearchService searchService,
                             ResourceService resourceService,
                             ResourceTypeService resourceTypeService,
                             ParserService parserPool,
                             DefaultFacetLabelService facetLabelService) {
        this.searchService = searchService;
        this.resourceService = resourceService;
        this.resourceTypeService = resourceTypeService;
        this.parserPool = parserPool;
        this.facetLabelService = facetLabelService;
    }

    @PostConstruct
    void initResourceTypesBrowseFields() { // TODO: move this to a bean to avoid running multiple times ??
        browseByMap = new TreeMap<>();
        labelsMap = new TreeMap<>();
        Map<String, Set<String>> aliasGroupBrowse = new TreeMap<>();
        Map<String, Map<String, String>> aliasGroupLabels = new TreeMap<>();
        for (ResourceType rt : resourceTypeService.getAllResourceType()) {
            Set<String> browseSet = new TreeSet<>();
            Map<String, Set<String>> sets = new TreeMap<>();
            Map<String, String> labels = new TreeMap<>();

            labels.put("resourceType", "Resource Type");
            for (IndexField f : rt.getIndexFields()) {
                sets.putIfAbsent(f.getResourceType().getName(), new HashSet<>());
                labels.put(f.getName(), f.getLabel());
                if (f.getLabel() != null) {
                    sets.get(f.getResourceType().getName()).add(f.getName());
                }
            }

            boolean flag = true;
            for (Map.Entry<String, Set<String>> entry : sets.entrySet()) {
                if (flag) {
                    browseSet.addAll(entry.getValue());
                    flag = false;
                } else {
                    browseSet.retainAll(entry.getValue());
                }
            }
            if (rt.getAliases() != null) {
                for (String alias : rt.getAliases()) {
                    if (aliasGroupBrowse.get(alias) == null) {
                        aliasGroupBrowse.put(alias, new TreeSet<>(browseSet));
                        aliasGroupLabels.put(alias, new TreeMap<>(labels));
                    } else {
                        aliasGroupBrowse.get(alias).retainAll(browseSet);
                        aliasGroupLabels.get(alias).keySet().retainAll(labels.keySet());
                    }
                }
            }

            labelsMap.put(rt.getName(), labels);
            browseByMap.put(rt.getName(), new ArrayList<>(browseSet));
            logger.debug("Generating browse fields for [{}]", rt.getName());
        }
        for (String alias : aliasGroupBrowse.keySet()) {
            browseByMap.put(alias, aliasGroupBrowse.get(alias).stream().sorted().collect(Collectors.toList()));
            labelsMap.put(alias, aliasGroupLabels.get(alias));
        }
    }

    @Override
    public <T> T get(String resourceTypeName, String field, String value, boolean throwOnNull) {
        Resource res;
        T ret;
        res = searchService.searchFields(resourceTypeName, new SearchService.KeyValue(field, value));
        if (throwOnNull && res == null) {
            throw new ResourceException(String.format("%s '%s' does not exist!", resourceTypeName, value), HttpStatus.NOT_FOUND);
        }
        ret = (T) parserPool.deserialize(res, getClassFromResourceType(resourceTypeName));
        return ret;
    }

    @Override
    public <T> T add(String resourceTypeName, T resource) {
        Class<?> clazz = getClassFromResourceType(resourceTypeName);
        if (!clazz.isInstance(resource)) {
            resource = (T) objectMapper.convertValue(resource, clazz);
        }

        ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeName);
        Resource res = new Resource();
        res.setResourceTypeName(resourceTypeName);
        res.setResourceType(resourceType);

        String id = null;
        try {
            id = ReflectUtils.getId(clazz, resource);
        } catch (Exception e) {
            logger.warn("Could not find field 'id'.", e);
        }
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
            ReflectUtils.setId(clazz, resource, id);
        }
        String payload = parserPool.serialize(resource, ParserService.ParserServiceTypes.fromString(resourceType.getPayloadType()));
        res.setPayload(payload);
        logger.info(LoggingUtils.addResource(resourceTypeName, id, resource));
        resourceService.addResource(res);

        return resource;
    }

    @Override
    public <T> T update(String resourceTypeName, String id, T resource) throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException {
        Class<?> clazz = getClassFromResourceType(resourceTypeName);
        resource = (T) objectMapper.convertValue(resource, clazz);

        String existingId = ReflectUtils.getId(clazz, resource);
        if (!id.equals(existingId)) {
            throw new ResourceException("Resource body id different than path id", HttpStatus.CONFLICT);
        }

        ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeName);
        Resource res = searchResource(resourceTypeName, id, true);
        String payload = parserPool.serialize(resource, ParserService.ParserServiceTypes.fromString(resourceType.getPayloadType()));
        res.setPayload(payload);
        logger.info(LoggingUtils.updateResource(resourceTypeName, id, resource));
        resourceService.updateResource(res);

        return resource;
    }

    @Override
    public <T> T delete(String resourceTypeName, String id) {
        Resource res = searchResource(resourceTypeName, id, true);
        logger.info(LoggingUtils.deleteResource(resourceTypeName, id, res));
        resourceService.deleteResource(res.getId());
        return (T) parserPool.deserialize(res, getClassFromResourceType(resourceTypeName));
    }

    @Override
    public <T> T get(String resourceTypeName, String id) {
        Resource res = searchResource(resourceTypeName, id, true);
        return (T) parserPool.deserialize(res, getClassFromResourceType(res.getResourceTypeName()));
    }

    @Override
    public <T> Browsing<T> getResults(FacetFilter filter) {
        Set<String> browseBy = new HashSet<>(filter.getBrowseBy());
        browseBy.addAll(browseByMap.get(filter.getResourceType()));
        filter.setBrowseBy(new ArrayList<>(browseBy));
        Browsing<T> browsing;
        browsing = convertToBrowsing(searchService.search(filter), filter.getResourceType());
        browsing.setFacets(facetLabelService.generateLabels(browsing.getFacets()));
        return browsing;
    }

    public <T> Browsing<T> getResultsWithoutFacets(FacetFilter filter) {
        return convertToBrowsing(searchService.search(filter), filter.getResourceType());
    }

    @Override
    public <T> Browsing<T> convertToBrowsing(@NotNull Paging<Resource> paging, String resourceTypeName) {
        Class<?> clazz = getClassFromResourceType(resourceTypeName);
        List<T> results;
        if (clazz != null) { // all resources are from the same resourceType
            results = (List<T>) paging.getResults()
                    .parallelStream()
                    .map(res -> (T) parserPool.deserialize(res, clazz))
                    .collect(Collectors.toList());
        } else { // mixed resources
            results = (List<T>) paging.getResults()
                    .stream()
                    .map(resource -> {
                        T item = null;
                        try {
                            item = (T) parserPool.deserialize(resource, getClassFromResourceType(resource.getResourceTypeName()));
                        } catch (Exception e) {
                            logger.warn("Problem encountered: ", e);
                        }
                        return item;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return new Browsing<>(paging, results, labelsMap.get(resourceTypeName));
    }

    @Override
    public <T> Map<String, List<T>> getResultsGrouped(FacetFilter filter, String category) {
        Map<String, List<T>> result = new HashMap<>();
        Class<?> clazz = getClassFromResourceType(filter.getResourceType());

        Map<String, List<Resource>> resources;
        try {
            resources = searchService.searchByCategory(filter, category);
            for (Map.Entry<String, List<Resource>> bucket : resources.entrySet()) {
                List<T> bucketResults = new ArrayList<>();
                for (Resource res : bucket.getValue()) {
                    if (clazz != null) {
                        bucketResults.add((T) parserPool.deserialize(res, clazz));
                    } else {
                        bucketResults.add((T) parserPool.deserialize(res, getClassFromResourceType(res.getResourceTypeName())));
                    }
                }
                result.put(bucket.getKey(), bucketResults);
            }
            return result;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ServiceException(e);
        }
    }

    @Override
    public Class<?> getClassFromResourceType(String resourceTypeName) {
        Class<?> tClass = null;
        try {
            ResourceType resourceType = resourceTypeService.getResourceType(resourceTypeName);
            if (resourceType == null) { // search was performed using alias
                return null;
            }
            tClass = Class.forName(resourceType.getProperty("class"));
        } catch (ClassNotFoundException e) {
            logger.warn(e.getMessage(), e);
            tClass = Map.class;
        } catch (NullPointerException e) {
            logger.error("Class property is not defined", e);
            throw new ServiceException(String.format("ResourceType [%s] does not have properties field", resourceTypeName));
        }
        return tClass;
    }

    @Override
    public Resource searchResource(String resourceTypeName, String id, boolean throwOnNull) {
        Resource res = searchService.searchFields(resourceTypeName, new SearchService.KeyValue("resource_internal_id", id));
        if (throwOnNull) {
            return Optional.ofNullable(res)
                    .orElseThrow(() -> new ResourceNotFoundException(id, resourceTypeName));
        }
        return res;
    }

    @Override
    public Resource searchResource(String resourceTypeName, SearchService.KeyValue... keyValues) {
        Resource res = searchService.searchFields(resourceTypeName, keyValues);
        return res;
    }

    public Map<String, List<String>> getBrowseByMap() {
        return browseByMap;
    }

    public void setBrowseByMap(Map<String, List<String>> browseByMap) {
        this.browseByMap = browseByMap;
    }

    // facets are pre-sorted by 'count' field
    public void sortFacets(List<Facet> facets, String field) {
        for (Iterator<Facet> iter = facets.listIterator(); iter.hasNext(); ) {
            Facet facet = iter.next();
            if (facet.getField().equals("catalogue_id") || facet.getField().equals(field)) {
                try {
                    facet.getValues().sort(Comparator.comparing(gr.uoa.di.madgik.registry.domain.Value::getLabel, String.CASE_INSENSITIVE_ORDER));
                } catch (NullPointerException e) {
                    facet.getValues().sort(Comparator.comparing(gr.uoa.di.madgik.registry.domain.Value::getValue, String.CASE_INSENSITIVE_ORDER));
                }
            }
        }
    }

    public List<String> getBrowseBy(String resourceType) {
        for (Map.Entry<String, List<String>> entry : browseByMap.entrySet()) {
            if (entry.getKey().equals(resourceType)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Map<String, String> getLabels(String resourceType) {
        for (Map.Entry<String, Map<String, String>> entry : labelsMap.entrySet()) {
            if (entry.getKey().equals(resourceType)) {
                return entry.getValue();
            }
        }
        return null;
    }

}
