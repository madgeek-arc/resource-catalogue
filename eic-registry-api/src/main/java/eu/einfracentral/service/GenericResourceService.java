package eu.einfracentral.service;

import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.SearchService;

import javax.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public interface GenericResourceService {
    <T> T get(String resourceTypeName, String field, String value, boolean throwOnNull);

    <T> Browsing<T> cqlQuery(FacetFilter filter);

    <T> Browsing<T> getResults(FacetFilter filter);

    <T> Browsing<T> convertToBrowsing(@NotNull Paging<Resource> paging, String resourceTypeName);

    <T> Map<String, List<T>> getResultsGrouped(FacetFilter filter, String category);

    <T> T add(String resourceTypeName, T resource);

    <T> T update(String resourceTypeName, String id, T resource) throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException;

    <T> T delete(String resourceTypeName, String id);

    <T> T get(String resourceTypeName, String id);

    Class<?> getClassFromResourceType(String resourceTypeName);

    Resource searchResource(String resourceTypeName, String id, boolean throwOnNull);

    Resource searchResource(String resourceTypeName, SearchService.KeyValue... keyValues);
}
