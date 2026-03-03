/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.manager;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.Datasource;
import gr.uoa.di.madgik.resourcecatalogue.dto.OpenAIREMetrics;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.OpenAIREDatasourceService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;


@Service
public class OpenAIREDatasourceManager implements OpenAIREDatasourceService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIREDatasourceManager.class);

    @Value("${openaire.ds.api:}")
    private String openaireAPI;
    @Value("${openaire.ds.metrics.validated:}")
    private String openaireMetricsValidated;
    @Value("${openaire.ds.metrics:}")
    private String openaireMetrics;

    private String[] getOpenAIREDatasourcesAsJSON(FacetFilter ff, String keywordField) {
        String[] pagination = createPagination(ff, keywordField);
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

    private String[] createPagination(FacetFilter ff, String keywordField) {
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
        if (keywordField == null) keywordField = "officialname";
        if (ff.getKeyword() != null && !ff.getKeyword().isEmpty()) {
            data = "{\"" + keywordField + "\": \"" + ff.getKeyword() + "\"}";
        }
        return new String[]{Integer.toString(page), Integer.toString(quantity), ordering, data};
    }

    private final WebClient webClient = WebClient.builder()
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    private String createHttpRequest(String url, String data) {
        WebClient.RequestBodySpec requestSpec = webClient.method(data != null ? HttpMethod.POST : HttpMethod.GET)
                .uri(url);

        WebClient.ResponseSpec responseSpec = (data != null)
                ? requestSpec.bodyValue(data).retrieve()
                : requestSpec.retrieve();

        return responseSpec.bodyToMono(String.class).block();
    }


    @Override
    public LinkedHashMap<String, Object> get(String id) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("id", id);
        String datasource = getOpenAIREDatasourcesAsJSON(ff, null)[1];
        if (datasource != null) {
            JSONObject obj = new JSONObject(datasource);
            JSONArray arr = obj.getJSONArray("datasourceInfo");
            if (arr != null) {
                if (arr.length() == 0) {
                    throw new CatalogueResourceNotFoundException(String.format("There is no OpenAIRE Datasource with the given id [%s]", id));
                } else {
                    JSONObject map = arr.getJSONObject(0);
                    Gson gson = new Gson();
                    JsonElement jsonObj = gson.fromJson(String.valueOf(map), JsonElement.class);
                    return transformOpenAIREToEOSCDatasource(jsonObj);
                }
            }
        }
        throw new CatalogueResourceNotFoundException(String.format("There is no OpenAIRE Datasource with the given id [%s]", id));
    }

    @Override
    public Map<Integer, List<LinkedHashMap<String, Object>>> getAll(FacetFilter ff) {
        Map<Integer, List<LinkedHashMap<String, Object>>> datasourceMap = new HashMap<>();
        List<LinkedHashMap<String, Object>> allDatasources = new ArrayList<>();
        String[] datasourcesAsJSON;
        if (ff.getKeyword() != null && !ff.getKeyword().isEmpty()) {
            datasourcesAsJSON = getOpenAIREDatasourcesAsJSON(ff, "id");
            if (datasourcesAsJSON.length == 0 || datasourcesAsJSON[0].equals("0")) {
                datasourcesAsJSON = getOpenAIREDatasourcesAsJSON(ff, "officialname");
            }
        } else {
            datasourcesAsJSON = getOpenAIREDatasourcesAsJSON(ff, "officialname");
        }

        int total = Integer.parseInt(datasourcesAsJSON[0]);
        String allOpenAIREDatasources = datasourcesAsJSON[1];
        if (allOpenAIREDatasources != null) {
            JSONObject obj = new JSONObject(allOpenAIREDatasources);
            JSONArray arr = obj.getJSONArray("datasourceInfo");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject map = arr.getJSONObject(i);
                Gson gson = new Gson();
                JsonElement jsonObj = gson.fromJson(String.valueOf(map), JsonElement.class);
                LinkedHashMap<String, Object> datasource = transformOpenAIREToEOSCDatasource(jsonObj);
                if (datasource != null) {
                    allDatasources.add(datasource);
                }
            }
            datasourceMap.put(total, allDatasources);
            return datasourceMap;
        }
        throw new CatalogueResourceNotFoundException("There are no OpenAIRE Datasources");
    }

    private LinkedHashMap<String, Object> transformOpenAIREToEOSCDatasource(JsonElement openaireDatasource) {
        LinkedHashMap<String, Object> datasource = new LinkedHashMap<>();
        String id = openaireDatasource.getAsJsonObject().get("id").getAsString().replaceAll("\"", "");
        datasource.put("id", id);
        return datasource;
    }

    public String getRegisterBy(String openaireDatasourceID) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("id", openaireDatasourceID);
        String datasource = getOpenAIREDatasourcesAsJSON(ff, null)[1];
        String registerBy = null;
        if (datasource != null) {
            JSONObject obj = new JSONObject(datasource);
            JSONArray arr = obj.getJSONArray("datasourceInfo");
            if (arr != null) {
                if (arr.length() == 0) {
                    throw new CatalogueResourceNotFoundException(String.format("There is no OpenAIRE Datasource with the given id [%s]", openaireDatasourceID));
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
        //TODO: waiting for new API call
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
