package gr.uoa.di.madgik.service;

import gr.uoa.di.madgik.domain.DatasourceBundle;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;

public interface DatasourceService extends ResourceService<DatasourceBundle, Authentication> {

    /**
     * Get the Datasource sub-profile of the specific Service of the specific Catalogue
     *
     * @param serviceId   Service ID
     * @param catalogueId Catalogue ID
     * @return {@link DatasourceBundle}
     */
    DatasourceBundle get(String serviceId, String catalogueId);

    /**
     * Update the specific Datasource
     *
     * @param datasourceBundle Datasource Bundle to be updated
     * @param comment          Optional comment of the update
     * @param auth             Authentication
     * @return {@link DatasourceBundle}
     */
    DatasourceBundle update(DatasourceBundle datasourceBundle, String comment, Authentication auth);

    /**
     * Verify (approve/reject) a Datasource.
     *
     * @param id     Datasource ID
     * @param status New status
     * @param active New active
     * @param auth   Authentication
     * @return {@link DatasourceBundle}
     */
    DatasourceBundle verifyDatasource(String id, String status, Boolean active, Authentication auth);

    /**
     * Update the specific DatasourceBundle
     *
     * @param datasourceBundle DatasourceBundle
     * @param auth             Authentication
     */
    void updateBundle(DatasourceBundle datasourceBundle, Authentication auth);

    /**
     * Create a FacetFilter for fetching Datasources
     *
     * @param allRequestParams {@link MultiValueMap} of all the Requested Parameters given
     * @param catalogueId      Catalogue ID
     * @return {@link FacetFilter}
     */
    FacetFilter createFacetFilterForFetchingDatasources(MultiValueMap<String, Object> allRequestParams,
                                                        String catalogueId);

    /**
     * Returns True/False according to if the specific Datasource
     * is already registered in the OpenAIRE Catalogue
     *
     * @param id The ID of the Datasource in creation
     * @return True/False
     */
    boolean isDatasourceRegisteredOnOpenAIRE(String id);

    /**
     * Get a Paging of DatasourceBundles of a specific Service of a specific Catalogue
     *
     * @param catalogueId Catalogue ID
     * @param serviceId   Service ID
     * @param auth        Authentication
     * @return {@link Paging}&lt;{@link DatasourceBundle}&gt;
     */
    Paging<DatasourceBundle> getResourceBundles(String catalogueId, String serviceId, Authentication auth);
}
