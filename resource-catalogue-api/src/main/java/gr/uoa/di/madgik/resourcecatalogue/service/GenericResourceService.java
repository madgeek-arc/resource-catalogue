package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.service.SearchService;

import javax.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public interface GenericResourceService {
    /**
     * @param resourceTypeName
     * @param field
     * @param value
     * @param throwOnNull
     * @param <T>
     * @return
     */
    <T> T get(String resourceTypeName, String field, String value, boolean throwOnNull);

    /**
     * @param filter
     * @param <T>
     * @return
     */
    <T> Browsing<T> getResults(FacetFilter filter);

    /**
     * @param filter
     * @param <T>
     * @return
     */
    <T> Browsing<T> getResultsWithoutFacets(FacetFilter filter);

    /**
     * @param paging
     * @param resourceTypeName
     * @param <T>
     * @return
     */
    <T> Browsing<T> convertToBrowsing(@NotNull Paging<Resource> paging, String resourceTypeName);

    /**
     * @param filter
     * @param category
     * @param <T>
     * @return
     */
    <T> Map<String, List<T>> getResultsGrouped(FacetFilter filter, String category);

    /**
     * @param resourceTypeName
     * @param resource
     * @param <T>
     * @return
     */
    <T> T add(String resourceTypeName, T resource);

    /**
     * @param resourceTypeName
     * @param id
     * @param resource
     * @param <T>
     * @return
     * @throws NoSuchFieldException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    <T> T update(String resourceTypeName, String id, T resource) throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException;

    /**
     * @param resourceTypeName
     * @param id
     * @param <T>
     * @return
     */
    <T> T delete(String resourceTypeName, String id);

    /**
     * @param resourceTypeName
     * @param id
     * @param <T>
     * @return
     */
    <T> T get(String resourceTypeName, String id);

    /**
     * @param resourceTypeName
     * @return
     */
    Class<?> getClassFromResourceType(String resourceTypeName);

    /**
     * @param resourceTypeName
     * @param id
     * @param throwOnNull
     * @return
     */
    Resource searchResource(String resourceTypeName, String id, boolean throwOnNull);

    /**
     * @param resourceTypeName
     * @param keyValues
     * @return
     */
    Resource searchResource(String resourceTypeName, SearchService.KeyValue... keyValues);

    /**
     * @param facets
     * @param field
     */
    void sortFacets(List<Facet> facets, String field);
}
