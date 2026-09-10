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

package gr.uoa.di.madgik.resourcecatalogue.manager.pids;

import gr.uoa.di.madgik.resourcecatalogue.config.properties.ResourceProperties;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.time.Duration;

/**
 * Validates a PID record's FDO-profile fields against the schema TypeAPI generates for that
 * profile, via {@code POST /v1/types/validate/{profile}} — before the record is PUT to the PID
 * service by {@link PidIssuer}. No authentication is required by TypeAPI.
 */
@Component
public class PidRecordValidator {

    private static final Logger logger = LoggerFactory.getLogger(PidRecordValidator.class);

    /**
     * TypeAPI always answers with HTTP 200; validity is encoded in the plain-text response body
     * instead — either exactly this string, or a bracketed list of violated-field messages
     * (e.g. {@code [$: required property 'publicContacts' not found]}).
     */
    private static final String VALID_RESPONSE_BODY = "Valid";

    private final WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(
                    HttpClient.create().responseTimeout(Duration.ofSeconds(10))))
            .build();

    /**
     * Validates {@code fields} (plus {@code pid} as the record's {@code id}) against the FDO
     * profile schema at {@code fdo.getProfile()}, resolved against {@code typeApiUrl}.
     *
     * @throws RuntimeException if the record is invalid per the FDO profile, or the validation
     *                          call itself fails.
     */
    public void validate(String typeApiUrl, String pid, PidFields fields, ResourceProperties.Fdo fdo) {
        String profile = (fdo != null) ? fdo.getProfile() : null;
        if (!StringUtils.hasText(profile)) {
            logger.debug("No FDO profile configured; skipping FDO profile validation for PID '{}'", pid);
            return;
        }

        URI uri = URI.create(String.join("/", typeApiUrl, "v1", "types", "validate", profile));
        String requestBody = buildFlatJson(pid, fields).toString();

        ResponseEntity<String> response;
        try {
            response = webClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class))
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Error while validating PID '" + pid + "' against FDO profile '"
                    + profile + "'", e);
        }

        String body = (response != null && response.getBody() != null) ? response.getBody().trim() : "";
        if (!VALID_RESPONSE_BODY.equals(body)) {
            throw new RuntimeException("FDO profile validation failed for PID '" + pid + "' against profile '"
                    + profile + "': " + body);
        }
        logger.debug("PID '{}' validated successfully against FDO profile '{}'", pid, profile);
    }

    private JSONObject buildFlatJson(String pid, PidFields fields) {
        JSONObject json = new JSONObject();
        json.put("id", pid);
        putIfPresent(json, "name", fields.name());
        putIfPresent(json, "description", fields.description());
        putIfPresent(json, "publishingDate", fields.publishingDate());
        putIfPresent(json, "type", fields.type());
        putIfPresent(json, "nodePID", fields.nodePID());
        putIfPresent(json, "resourceOwner", fields.resourceOwner());
        if (fields.publicContacts() != null && !fields.publicContacts().isEmpty()) {
            json.put("publicContacts", new JSONArray(fields.publicContacts()));
        }
        return json;
    }

    private void putIfPresent(JSONObject json, String key, String value) {
        if (StringUtils.hasText(value)) {
            json.put(key, value);
        }
    }
}
