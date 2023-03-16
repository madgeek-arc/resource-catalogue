package eu.einfracentral.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.service.GenericResourceService;
import eu.einfracentral.utils.FacetLabelService;
import eu.einfracentral.utils.ReflectUtils;
import eu.einfracentral.utils.LoggingUtils;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import eu.openminted.registry.core.service.ParserService.ParserServiceTypes;
import java.lang.reflect.InvocationTargetException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

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

    protected GenericManager(SearchService searchService, ResourceService resourceService,
                             ResourceTypeService resourceTypeService, ParserService parserPool,
                             FacetLabelService facetLabelService) {
        this.searchService = searchService;
        this.resourceService = resourceService;
        this.resourceTypeService = resourceTypeService;
        this.parserPool = parserPool;
        this.facetLabelService = facetLabelService;
    }

    @PostConstruct
    void initResourceTypesBrowseFields() {
        this.browseByMap = new HashMap();
        this.labelsMap = new HashMap();
        Map<String, Set<String>> aliasGroupBrowse = new HashMap();
        Map<String, Map<String, String>> aliasGroupLabels = new HashMap();
        Iterator var3 = this.resourceTypeService.getAllResourceType().iterator();

        while(var3.hasNext()) {
            ResourceType rt = (ResourceType)var3.next();
            Set<String> browseSet = new HashSet();
            Map<String, Set<String>> sets = new HashMap();
            Map<String, String> labels = new HashMap();
            labels.put("resourceType", "Resource Type");
            Iterator var8 = rt.getIndexFields().iterator();

            while(var8.hasNext()) {
                IndexField f = (IndexField)var8.next();
                sets.putIfAbsent(f.getResourceType().getName(), new HashSet());
                labels.put(f.getName(), f.getLabel());
                if (f.getLabel() != null) {
                    ((Set)sets.get(f.getResourceType().getName())).add(f.getName());
                }
            }

            this.labelsMap.put(rt.getName(), labels);
            boolean flag = true;
            Iterator var13 = sets.entrySet().iterator();

            while(var13.hasNext()) {
                Map.Entry<String, Set<String>> entry = (Map.Entry)var13.next();
                if (flag) {
                    browseSet.addAll((Collection)entry.getValue());
                    flag = false;
                } else {
                    browseSet.retainAll((Collection)entry.getValue());
                }
            }

            if (rt.getAliasGroup() != null) {
                if (aliasGroupBrowse.get(rt.getAliasGroup()) == null) {
                    aliasGroupBrowse.put(rt.getAliasGroup(), browseSet);
                    aliasGroupLabels.put(rt.getAliasGroup(), labels);
                } else {
                    ((Set)aliasGroupBrowse.get(rt.getAliasGroup())).retainAll(browseSet);
                    ((Map)aliasGroupLabels.get(rt.getAliasGroup())).keySet().retainAll(labels.keySet());
                }
            }

            List<String> browseBy = new ArrayList(browseSet);
            Collections.sort(browseBy);
            this.browseByMap.put(rt.getName(), browseBy);
            logger.debug("Generating browse fields for [{}]", rt.getName());
        }

        var3 = aliasGroupBrowse.keySet().iterator();

        while(var3.hasNext()) {
            String alias = (String)var3.next();
            this.browseByMap.put(alias, (List)((Set)aliasGroupBrowse.get(alias)).stream().sorted().collect(Collectors.toList()));
            this.labelsMap.put(alias, (Map)aliasGroupLabels.get(alias));
        }

    }

    public <T> T get(String resourceTypeName, String field, String value, boolean throwOnNull) {
        try {
            Resource res = this.searchService.searchId(resourceTypeName, new SearchService.KeyValue[]{new SearchService.KeyValue(field, value)});
            if (throwOnNull && res == null) {
                throw new ResourceException(String.format("%s '%s' does not exist!", resourceTypeName, value), HttpStatus.NOT_FOUND);
            } else {
                T ret = (T) this.parserPool.deserialize(res, this.getClassFromResourceType(resourceTypeName));
                return ret;
            }
        } catch (UnknownHostException var8) {
            logger.error(var8.getMessage(), var8);
            throw new ServiceException(var8);
        }
    }

    public <T> T add(String resourceTypeName, T resource) {
        Class<?> clazz = this.getClassFromResourceType(resourceTypeName);
        if (!clazz.isInstance(resource)) {
            resource = (T) this.objectMapper.convertValue(resource, clazz);
        }

        ResourceType resourceType = this.resourceTypeService.getResourceType(resourceTypeName);
        Resource res = new Resource();
        res.setResourceTypeName(resourceTypeName);
        res.setResourceType(resourceType);
        String id = null;

        try {
            id = ReflectUtils.getId(clazz, resource);
        } catch (Exception var8) {
            logger.warn("Could not find field 'id'.", var8);
        }

        if (id == null || id.replaceAll("\\s", "").isEmpty()) {
            id = UUID.randomUUID().toString();
            ReflectUtils.setId(clazz, resource, id);
        }

        String payload = this.parserPool.serialize(resource, ParserServiceTypes.fromString(resourceType.getPayloadType()));
        res.setPayload(payload);
        logger.info(LoggingUtils.addResource(resourceTypeName, id, resource));
        this.resourceService.addResource(res);
        return resource;
    }

    public <T> T update(String resourceTypeName, String id, T resource) throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException {
        Class<?> clazz = this.getClassFromResourceType(resourceTypeName);
        resource = (T) this.objectMapper.convertValue(resource, clazz);
        String existingId = ReflectUtils.getId(clazz, resource);
        if (!id.equals(existingId)) {
            throw new ResourceException("Resource body id different than path id", HttpStatus.CONFLICT);
        } else {
            ResourceType resourceType = this.resourceTypeService.getResourceType(resourceTypeName);
            Resource res = this.searchResource(resourceTypeName, id, true);
            String payload = this.parserPool.serialize(resource, ParserServiceTypes.fromString(resourceType.getPayloadType()));
            res.setPayload(payload);
            logger.info(LoggingUtils.updateResource(resourceTypeName, id, resource));
            this.resourceService.updateResource(res);
            return resource;
        }
    }

    public <T> T delete(String resourceTypeName, String id) {
        Resource res = this.searchResource(resourceTypeName, id, true);
        logger.info(LoggingUtils.deleteResource(resourceTypeName, id, res));
        this.resourceService.deleteResource(res.getId());
        return (T) this.parserPool.deserialize(res, this.getClassFromResourceType(resourceTypeName));
    }

    public <T> T get(String resourceTypeName, String id) {
        Resource res = this.searchResource(resourceTypeName, id, true);
        return (T) this.parserPool.deserialize(res, this.getClassFromResourceType(res.getResourceTypeName()));
    }

    public <T> Browsing<T> cqlQuery(FacetFilter filter) {
        filter.setBrowseBy((List)this.browseByMap.get(filter.getResourceType()));
        return this.convertToBrowsing(this.searchService.cqlQuery(filter), filter.getResourceType());
    }

    public <T> Browsing<T> getResults(FacetFilter filter) {
        filter.setBrowseBy((List)this.browseByMap.get(filter.getResourceType()));

        try {
            Browsing<T> browsing = this.convertToBrowsing(this.searchService.search(filter), filter.getResourceType());
            browsing.setFacets(facetLabelService.generateLabels(browsing.getFacets()));
            return browsing;
        } catch (UnknownHostException var4) {
            throw new ServiceException(var4);
        }
    }

    public <T> Browsing<T> convertToBrowsing(Paging<Resource> paging, String resourceTypeName) {
        Class<?> clazz = this.getClassFromResourceType(resourceTypeName);
        List results;
        if (clazz != null) {
            results = (List)paging.getResults().parallelStream().map((res) -> {
                return this.parserPool.deserialize(res, clazz);
            }).collect(Collectors.toList());
        } else {
            results = (List)paging.getResults().stream().map((resource) -> {
                return this.parserPool.deserialize(resource, this.getClassFromResourceType(resource.getResourceTypeName()));
            }).collect(Collectors.toList());
        }

        return new Browsing(paging, results, (Map)this.labelsMap.get(resourceTypeName));
    }

    public <T> Map<String, List<T>> getResultsGrouped(FacetFilter filter, String category) {
        Map<String, List<T>> result = new HashMap();
        Class<?> clazz = this.getClassFromResourceType(filter.getResourceType());

        try {
            Map<String, List<Resource>> resources = this.searchService.searchByCategory(filter, category);
            Iterator var6 = resources.entrySet().iterator();

            while(var6.hasNext()) {
                Map.Entry<String, List<Resource>> bucket = (Map.Entry)var6.next();
                List<T> bucketResults = new ArrayList();
                Iterator var9 = ((List)bucket.getValue()).iterator();

                while(var9.hasNext()) {
                    Resource res = (Resource)var9.next();
                    bucketResults.add((T) this.parserPool.deserialize(res, clazz));
                }

                result.put((String)bucket.getKey(), bucketResults);
            }

            return result;
        } catch (Exception var11) {
            logger.error(var11.getMessage(), var11);
            throw new ServiceException(var11);
        }
    }

    public Class<?> getClassFromResourceType(String resourceTypeName) {
        Class<?> tClass = null;

        try {
            ResourceType resourceType = this.resourceTypeService.getResourceType(resourceTypeName);
            if (resourceType == null) {
                return null;
            }

            tClass = Class.forName(resourceType.getProperty("class"));
        } catch (ClassNotFoundException var4) {
            logger.error(var4.getMessage(), var4);
        } catch (NullPointerException var5) {
            logger.error("Class property is not defined", var5);
            throw new ServiceException(String.format("ResourceType [%s] does not have properties field", resourceTypeName));
        }

        return tClass;
    }

    public Resource searchResource(String resourceTypeName, String id, boolean throwOnNull) {
        Resource res = null;

        try {
            res = this.searchService.searchId(resourceTypeName, new SearchService.KeyValue[]{new SearchService.KeyValue("resource_internal_id", id)});
        } catch (UnknownHostException var6) {
            logger.error(var6.getMessage(), var6);
        }

        return throwOnNull ? (Resource)Optional.ofNullable(res).orElseThrow(() -> {
            return new ResourceNotFoundException(id, resourceTypeName);
        }) : res;
    }

    public Resource searchResource(String resourceTypeName, SearchService.KeyValue... keyValues) {
        Resource res = null;

        try {
            res = this.searchService.searchId(resourceTypeName, keyValues);
        } catch (UnknownHostException var5) {
            logger.error(var5.getMessage(), var5);
        }

        return res;
    }

    public Map<String, List<String>> getBrowseByMap() {
        return this.browseByMap;
    }

    public void setBrowseByMap(Map<String, List<String>> browseByMap) {
        this.browseByMap = browseByMap;
    }
}

