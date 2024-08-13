package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.Datasource;
import gr.uoa.di.madgik.resourcecatalogue.dto.OpenAIREMetrics;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface OpenAIREDatasourceService {

    /**
     * Get a specific external Datasource given its ID.
     *
     * @param id Datasource ID
     * @return {@link Datasource}
     * @throws IOException If an input or output exception occurred
     */
    Datasource get(String id) throws IOException;

    /**
     * Get a list of all Datasources.
     *
     * @param ff FacetFilter
     * @return {@link Map <>}
     * @throws IOException If an input or output exception occurred
     */
    Map<Integer, List<Datasource>> getAll(FacetFilter ff) throws IOException;

    /**
     * Returns various metrics of an OpenAIRE Datasource, given the EOSC Datasource ID
     *
     * @param id Datasource ID
     * @return {@link Boolean}
     */
    OpenAIREMetrics getMetrics(String id);
}
