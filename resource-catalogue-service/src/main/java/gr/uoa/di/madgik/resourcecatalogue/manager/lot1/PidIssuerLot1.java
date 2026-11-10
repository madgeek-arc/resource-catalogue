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

package gr.uoa.di.madgik.resourcecatalogue.manager.lot1;

import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.ResourceProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuerConfig;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidRecordValidator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * LOT1 PID issuer: registers the minimal record (HS_ADMIN, ID and resolve URLs) used before the FDO-profile
 * records of the beyond profile. Everything else (auth, deletion, lookup) is inherited from {@link PidIssuer}.
 */
@Primary
@Component
public class PidIssuerLot1 extends PidIssuer {

    public PidIssuerLot1(CatalogueProperties properties, PidRecordValidator pidRecordValidator) {
        super(properties, pidRecordValidator);
    }

    @Override
    public void postPID(Bundle bundle, String resourceType, List<String> customResolveEndpoints) {
        String pid = bundle.getId();
        ResourceProperties resourceProperties = properties.getResourcePropertiesFromPrefix(pid.split("/")[0]);
        PidIssuerConfig config = resourceProperties.getPidIssuer();
        WebClient webClient = createWebClient(config);
        HttpHeaders headers = createHeaders(config);
        String payload;
        if (customResolveEndpoints != null && !customResolveEndpoints.isEmpty()) {
            payload = createPIDForLot1(pid, config, customResolveEndpoints, true);
        } else {
            payload = createPIDForLot1(pid, config, resourceProperties.getResolveEndpoints(), false);
        }
        exchange(payload, headers, config, pid, webClient, HttpMethod.PUT);
    }

    private String createPIDForLot1(String pid, PidIssuerConfig config, List<String> resolveEndpoints, boolean isCustom) {
        JSONObject data = new JSONObject();
        JSONArray values = new JSONArray();
        JSONObject hs_admin = new JSONObject();
        JSONObject hs_admin_data = new JSONObject();
        JSONObject hs_admin_data_value = new JSONObject();
        JSONObject id = new JSONObject();
        JSONObject id_data = new JSONObject();

        hs_admin_data_value.put("handle", config.getUser());
        hs_admin_data_value.put("index", Integer.parseInt(config.getUserIndex()));
        hs_admin_data_value.put("permissions", "011111110011");
        hs_admin_data.put("format", "admin");
        hs_admin_data.put("value", hs_admin_data_value);
        hs_admin.put("index", 100);
        hs_admin.put("type", "HS_ADMIN");
        hs_admin.put("data", hs_admin_data);
        values.put(hs_admin);
        id_data.put("format", "string");
        id_data.put("value", pid);
        id.put("index", 1);
        id.put("type", "ID");
        id.put("data", id_data);
        values.put(id);
        if (resolveEndpoints != null && !resolveEndpoints.isEmpty()) {
            int index = 2;
            for (String endpoint : resolveEndpoints) {
                JSONObject resolveUrls = new JSONObject();
                JSONObject resolveUrl_data = new JSONObject();
                resolveUrl_data.put("format", "string");
                if (isCustom) {
                    resolveUrl_data.put("value", endpoint);
                } else {
                    //FIXME: temporary solution with the fewest changes
                    if (endpoint.startsWith("https://search.marketplace.sandbox.eosc-beyond.eu")) {
                        String encodedSlash = pid.replace("/", "%252F");
                        resolveUrl_data.put("value", String.join("/", endpoint, encodedSlash));
                    } else {
                        resolveUrl_data.put("value", String.join("/", endpoint, pid));
                    }
                }
                resolveUrls.put("index", index);
                resolveUrls.put("type", "URL");
                resolveUrls.put("data", resolveUrl_data);
                values.put(resolveUrls);
                index++;
            }
        }
        data.put("values", values);
        return data.toString();
    }
}
