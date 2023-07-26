package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Datasource;
import eu.einfracentral.domain.DatasourceBundle;
import eu.openminted.registry.core.domain.FacetFilter;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface DatasourceService<T, U extends Authentication> extends ResourceService<T, Authentication> {

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
     * Verify (approve/reject) a Datasource.
     *
     * @param id - The ID of the Datasource
     * @param status - New status
     * @param active - New active
     * @param auth - Authentication
     * @return {@link DatasourceBundle}
     */
    DatasourceBundle verifyDatasource(String id, String status, Boolean active, Authentication auth);
}
