package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Datasource;
import eu.einfracentral.dto.OpenAIREMetrics;
import eu.openminted.registry.core.domain.FacetFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface OpenAIREDatasourceService {

    /**
     * Get a specific external Datasource given its ID.
     *
     * @param id - The ID of the Datasource
     * @return {@link Datasource}
     * @throws IOException If an input or output exception occurred
     */
    Datasource get(String id) throws IOException;

    /**
     * Get a list of all Datasources.
     *
     * @param ff - FacetFilter
     * @return {@link Map <>}
     * @throws IOException If an input or output exception occurred
     */
    Map<Integer, List<Datasource>> getAll(FacetFilter ff) throws IOException;

    /**
     * Returns various metrics of an OpenAIRE Datasource, given the EOSC Datasource ID
     *
     * @param id - The ID of the Datasource
     * @return {@link Boolean}
     */
    OpenAIREMetrics getMetrics(String id);
}
