package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Datasource;
import eu.openminted.registry.core.domain.FacetFilter;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface DatasourceService<T, U extends Authentication> extends ResourceService<T, Authentication> {

    Map<Integer, List<Datasource>> getAllOpenAIREDatasources(FacetFilter ff) throws IOException;

    String[] getOpenAIREDatasourcesAsJSON(FacetFilter ff) throws IOException;

    Datasource getOpenAIREDatasourceById(String datasourceId) throws IOException;

    String createHttpRequest(String url, String data);

    boolean isDatasourceRegisteredOnOpenAIRE(String eoscId);
}
