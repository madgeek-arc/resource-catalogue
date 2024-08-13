package gr.uoa.di.madgik.resourcecatalogue.manager;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.Datasource;
import gr.uoa.di.madgik.resourcecatalogue.dto.OpenAIREMetrics;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.OpenAIREDatasourceService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class OpenAIREDatasourceManager implements OpenAIREDatasourceService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIREDatasourceManager.class);

    private final String openaireAPI;
    private final String openaireMetricsValidated;
    private final String openaireMetrics;

    public OpenAIREDatasourceManager(@Value("${openaire.dsm.api}") String openaireAPI,
                                     @Value("${openaire.ds.metrics.validated}") String openaireMetricsValidated,
                                     @Value("${openaire.ds.metrics}") String openaireMetrics) {
        this.openaireAPI = openaireAPI;
        this.openaireMetricsValidated = openaireMetricsValidated;
        this.openaireMetrics = openaireMetrics;
    }

    private String[] getOpenAIREDatasourcesAsJSON(FacetFilter ff) {
        String[] pagination = createPagination(ff);
        int page = Integer.parseInt(pagination[0]);
        int quantity = Integer.parseInt(pagination[1]);
        String ordering = pagination[2];
        String data = pagination[3];
        String url = openaireAPI + "openaire/ds/searchdetails/" + page + "/" + quantity + "?order=" + ordering + "&requestSortBy=id";
        String response = createHttpRequest(url, data);
        if (response != null) {
            JSONObject obj = new JSONObject(response);
            Gson gson = new Gson();
            JsonElement jsonObj = gson.fromJson(String.valueOf(obj), JsonElement.class);
            String total = jsonObj.getAsJsonObject().get("header").getAsJsonObject().get("total").toString();
            jsonObj.getAsJsonObject().remove("header");
            return new String[]{total, jsonObj.toString()};
        }
        return new String[]{};
    }

    private String[] createPagination(FacetFilter ff) {
        int page;
        int quantity = ff.getQuantity();
        if (ff.getFrom() >= quantity) {
            page = ff.getFrom() / quantity;
        } else {
            page = ff.getFrom() / 10;
        }
        String ordering = "ASCENDING";
        if (ff.getOrderBy() != null) {
            String order = ff.getOrderBy().get(ff.getOrderBy().keySet().toArray()[0]).toString();
            if (order.contains("desc")) {
                ordering = "DESCENDING";
            }
        }
        String data = "{}";
        if (ff.getFilter() != null && !ff.getFilter().isEmpty()) {
            page = 0;
            quantity = 10;
            if (ff.getFilter().containsKey("id")) {
                data = "{  \"id\": \"" + ff.getFilter().get("id") + "\"}";
            }
        }
        if (ff.getKeyword() != null && !ff.getKeyword().equals("")) {
            data = "{  \"officialname\": \"" + ff.getKeyword() + "\"}";
        }
        return new String[]{Integer.toString(page), Integer.toString(quantity), ordering, data};
    }

    private String createHttpRequest(String url, String data) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("accept", "application/json");
        headers.add("Content-Type", "application/json");
        HttpEntity<String> entity;
        HttpMethod httpMethod;
        if (data != null) {
            entity = new HttpEntity<>(data, headers);
            httpMethod = HttpMethod.POST;
        } else {
            entity = new HttpEntity<>(headers);
            httpMethod = HttpMethod.GET;
        }
        return restTemplate.exchange(url, httpMethod, entity, String.class).getBody();
    }

    @Override

    public Datasource get(String id) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("id", id);
        String datasource = getOpenAIREDatasourcesAsJSON(ff)[1];
        if (datasource != null) {
            JSONObject obj = new JSONObject(datasource);
            JSONArray arr = obj.getJSONArray("datasourceInfo");
            if (arr != null) {
                if (arr.length() == 0) {
                    throw new ResourceNotFoundException(String.format("There is no OpenAIRE Datasource with the given id [%s]", id));
                } else {
                    JSONObject map = arr.getJSONObject(0);
                    Gson gson = new Gson();
                    JsonElement jsonObj = gson.fromJson(String.valueOf(map), JsonElement.class);
                    return transformOpenAIREToEOSCDatasource(jsonObj);
                }
            }
        }
        throw new ResourceNotFoundException(String.format("There is no OpenAIRE Datasource with the given id [%s]", id));
    }

    @Override
    public Map<Integer, List<Datasource>> getAll(FacetFilter ff) {
        Map<Integer, List<Datasource>> datasourceMap = new HashMap<>();
        List<Datasource> allDatasources = new ArrayList<>();
        String[] datasourcesAsJSON = getOpenAIREDatasourcesAsJSON(ff);
        int total = Integer.parseInt(datasourcesAsJSON[0]);
        String allOpenAIREDatasources = datasourcesAsJSON[1];
        if (allOpenAIREDatasources != null) {
            JSONObject obj = new JSONObject(allOpenAIREDatasources);
            JSONArray arr = obj.getJSONArray("datasourceInfo");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject map = arr.getJSONObject(i);
                Gson gson = new Gson();
                JsonElement jsonObj = gson.fromJson(String.valueOf(map), JsonElement.class);
                Datasource datasource = transformOpenAIREToEOSCDatasource(jsonObj);
                if (datasource != null) {
                    allDatasources.add(datasource);
                }
            }
            datasourceMap.put(total, allDatasources);
            return datasourceMap;
        }
        throw new ResourceNotFoundException("There are no OpenAIRE Datasources");
    }

    private Datasource transformOpenAIREToEOSCDatasource(JsonElement openaireDatasource) {
        Datasource datasource = new Datasource();
        String id = openaireDatasource.getAsJsonObject().get("id").getAsString().replaceAll("\"", "");
        datasource.setId(id);
        return datasource;
    }

    public String getRegisterBy(String openaireDatasourceID) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("id", openaireDatasourceID);
        String datasource = getOpenAIREDatasourcesAsJSON(ff)[1];
        String registerBy = null;
        if (datasource != null) {
            JSONObject obj = new JSONObject(datasource);
            JSONArray arr = obj.getJSONArray("datasourceInfo");
            if (arr != null) {
                if (arr.length() == 0) {
                    throw new ResourceNotFoundException(String.format("There is no OpenAIRE Datasource with the given id [%s]", openaireDatasourceID));
                } else {
                    JSONObject map = arr.getJSONObject(0);
                    Gson gson = new Gson();
                    JsonElement jsonObj = gson.fromJson(String.valueOf(map), JsonElement.class);
                    try {
                        registerBy = jsonObj.getAsJsonObject().get("registeredby") != JsonNull.INSTANCE ? jsonObj.getAsJsonObject().get("registeredby").getAsString() : null;
                    } catch (UnsupportedOperationException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        }
        return registerBy;
    }

    @Override
    public OpenAIREMetrics getMetrics(String id) {
        String openaireDatasourceId = getOpenAIREDatasourceIdByEOSCDatasourceId(id);
        if (openaireDatasourceId != null) {
            if (isMetricsValid(openaireDatasourceId)) {
                return fetchMetrics(openaireDatasourceId);
            }
        }
        return null;
    }

    public String getOpenAIREDatasourceIdByEOSCDatasourceId(String id) {
        // API call from Italians
        return "opendoar____::1106";
    }

    private boolean isMetricsValid(String id) {
        String url = String.format(openaireAPI + openaireMetricsValidated, id);
        String response = createHttpRequest(url, null);
        return response != null && response.equalsIgnoreCase("true");
    }

    private OpenAIREMetrics fetchMetrics(String id) {
        OpenAIREMetrics openAIREMetrics = new OpenAIREMetrics();
        String url = String.format(openaireAPI + openaireMetrics, id);
        String response = createHttpRequest(url, null);
        if (response != null) {
            JSONObject obj = new JSONObject(response).optJSONObject("metricsNumbers");
            if (obj != null) {
                openAIREMetrics.setPageViews(obj.optInt("pageviews"));
                openAIREMetrics.setTotalDownloads(obj.optInt("total_downloads"));
                openAIREMetrics.setTotalOpenaireDownloads(obj.optInt("total_openaire_downloads"));
                openAIREMetrics.setTotalViews(obj.optInt("total_openaire_views"));
                openAIREMetrics.setTotalOpenaireViews(obj.optInt("total_views"));
            }
        }
        return openAIREMetrics;
    }
}
