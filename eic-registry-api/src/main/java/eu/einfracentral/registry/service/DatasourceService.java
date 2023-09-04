package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Datasource;
import eu.einfracentral.domain.DatasourceBundle;
import eu.openminted.registry.core.domain.FacetFilter;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface DatasourceService<T, U extends Authentication> extends ResourceService<T, Authentication> {

    /**
     * Get the Datasource sub-profile of the specific Service of the specific Catalogue
     *
     * @param serviceId - The ID of the Service
     * @param catalogueId - The ID of the Catalogue
     * @return {@link DatasourceBundle}
     */
    DatasourceBundle get(String serviceId, String catalogueId);

    /**
     * Get the Datasource sub-profile of the specific Service of the specific Catalogue
     *
     * @param datasourceBundle - Datasource Bundle to be updated
     * @param comment - Optional comment of the update
     * @param auth - Authentication
     * @return {@link DatasourceBundle}
     */
    DatasourceBundle update(DatasourceBundle datasourceBundle, String comment, Authentication auth);

    /**
     * Get a list of all registered OpenAIRE Datasources.
     *
     * @param ff - FacetFilter
     * @return {@link Map<>}
     * @throws IOException  If an input or output exception occurred
     */
    Map<Integer, List<Datasource>> getAllOpenAIREDatasources(FacetFilter ff) throws IOException;

    /**
     * Get a specific OpenAIRE Datasource given its ID.
     *
     * @param datasourceId - The ID of the Datasource
     * @throws IOException  If an input or output exception occurred
     * @return {@link Datasource}
     */
    Datasource getOpenAIREDatasourceById(String datasourceId) throws IOException;

    /**
     * Returns True/False according to if the specific Datasource
     * is already registered in the OpenAIRE Catalogue
     *
     * @param eoscId - The ID of the Datasource in creation
     * @return True/False
     */
    boolean isDatasourceRegisteredOnOpenAIRE(String eoscId);

    /**
     * Verify (approve/reject) a Datasource.
     *
     * @param id - The ID of the Datasource
     * @param status - New status
     * @param active - New active
     * @param auth - Authentication
     * @return {@link DatasourceBundle}
     */
    DatasourceBundle verifyDatasource(String id, String status, Boolean active, Authentication auth);

    /**
     * Create a FacetFilter for fetching Datasources
     *
     * @param allRequestParams - All the @RequestParams given
     * @param catalogueId - The ID of the Catalogue
     * @return {@link FacetFilter}
     */
    FacetFilter createFacetFilterForFetchingDatasources(MultiValueMap<String, Object> allRequestParams, String catalogueId);
}
