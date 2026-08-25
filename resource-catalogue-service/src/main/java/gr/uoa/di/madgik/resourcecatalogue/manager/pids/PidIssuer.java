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

import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.ResourceProperties;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import java.io.FileInputStream;
import java.io.FileReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PidIssuer {

    private static final Logger logger = LoggerFactory.getLogger(PidIssuer.class);

    private static final int HS_ADMIN_INDEX = 100;
    private static final int FDO_TYPE_INDEX = 9991;
    private static final int FDO_PROFILE_INDEX = 9992;
    private static final int FDO_DATA_INDEX = 9993;
    private static final int FDO_VERSION_INDEX = 9994;
    private static final String ORGANISATION_RESOURCE_TYPE = "organisation";

    private final CatalogueProperties properties;

    public PidIssuer(CatalogueProperties properties) {
        this.properties = properties;
    }

    public void postPID(Bundle bundle, String resourceType, List<String> customResolveEndpoints) {
        String pid = bundle.getId();
        ResourceProperties resourceProperties = properties.getResourcePropertiesForResourceType(resourceType);
        PidIssuerConfig config = resourceProperties.getPidIssuer();
        WebClient webClient = createWebClient(config);
        HttpHeaders headers = createHeaders(config);

        // Custom endpoints (e.g. LOT1 supplying its own list) replace the configured ones entirely
        // rather than adding to them; this project's own pid.yml always configures exactly one.
        List<String> resolveEndpoints = (customResolveEndpoints != null && !customResolveEndpoints.isEmpty())
                ? customResolveEndpoints
                : resourceProperties.getResolveEndpoints();

        String payload = ORGANISATION_RESOURCE_TYPE.equals(resourceType)
                ? createOrganisationPID(pid, config, bundle.getPayload(), resolveEndpoints, resourceProperties.getFdo())
                : createPID(pid, config, bundle.getPayload(), resolveEndpoints, resourceProperties.getFdo());
        exchange(payload, headers, config, pid, webClient, HttpMethod.PUT);
    }

    public void deletePID(String pid) {
        String prefix = pid.split("/")[0];
        ResourceProperties resourceProperties = properties.getResourcePropertiesFromPrefix(prefix);
        PidIssuerConfig config = resourceProperties.getPidIssuer();
        WebClient webClient = createWebClient(config);
        HttpHeaders headers = createHeaders(config);
        exchange(null, headers, config, pid, webClient, HttpMethod.DELETE);
    }

    private WebClient createWebClient(PidIssuerConfig config) {
        if (config.getAuth() != null) {
            if (config.getAuth().isSelfSignedCert()) {
                return createSelfSignedWebClient(config.getAuth());
            } else {
                return createCertBasedWebClient(
                        config.getAuth().getClientCert(),
                        config.getAuth().getClientKey());
            }
        }
        return WebClient.builder().build();
    }

    /**
     * Builds a WebClient for PID services whose server certificate is self-signed.
     * <p>
     * Server trust: if {@code auth.serverCert} is set, only that certificate is trusted.
     * Otherwise all certificates are trusted and a warning is logged — this is unsafe on
     * untrusted networks and should only be used in development or tightly controlled environments.
     * <p>
     * Client authentication: if {@code clientCert} and {@code clientKey} are configured,
     * the client certificate is presented (mTLS). Otherwise the connection relies on the
     * {@code Authorization} header (basic auth or none).
     * <p>
     * Hostname verification is disabled because self-signed certificates typically lack
     * valid Subject Alternative Names.
     */
    private WebClient createSelfSignedWebClient(PidIssuerConfig.IssuerCertificateAuthenticationConfig auth) {
        try {
            SslContextBuilder builder = SslContextBuilder.forClient();

            // Configure server trust
            if (auth.getServerCert() != null && !auth.getServerCert().isBlank()) {
                try (FileInputStream serverCertStream = new FileInputStream(auth.getServerCert())) {
                    builder.trustManager(serverCertStream);
                }
            } else {
                logger.warn("selfSignedCert is true but no serverCert path is configured — "
                        + "trusting all certificates. This is unsafe on untrusted networks.");
                builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
            }

            // Configure client certificate if provided (mTLS)
            if (!auth.getClientKey().isBlank() && !auth.getClientCert().isBlank()) {
                PrivateKey privateKey = loadPrivateKey(auth.getClientKey());
                X509Certificate certificate = loadCertificate(auth.getClientCert());
                builder.keyManager(privateKey, certificate);
            }

            SslContext sslContext = builder.build();
            HttpClient httpClient = HttpClient.create()
                    .secure(spec -> spec
                            .sslContext(sslContext)
                            .handlerConfigurator(handler -> disableHostnameVerification(handler.engine())));
            return WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create WebClient for self-signed certificate", e);
        }
    }

    /**
     * Builds a WebClient for mutual TLS (mTLS) against a CA-signed server certificate.
     * The server certificate is verified against the JVM default trust store, and hostname
     * verification is left enabled.
     */
    private WebClient createCertBasedWebClient(String certPath, String keyPath) {
        try {
            PrivateKey privateKey = loadPrivateKey(keyPath);
            X509Certificate certificate = loadCertificate(certPath);

            SslContext sslContext = SslContextBuilder.forClient()
                    .keyManager(privateKey, certificate)
                    .build();
            HttpClient httpClient = HttpClient.create()
                    .secure(spec -> spec.sslContext(sslContext));
            return WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Error configuring WebClient with PEM files", e);
        }
    }

    private X509Certificate loadCertificate(String certPath) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (FileInputStream certStream = new FileInputStream(certPath)) {
            return (X509Certificate) cf.generateCertificate(certStream);
        }
    }

    private PrivateKey loadPrivateKey(String keyPath) throws Exception {
        try (PEMParser pemParser = new PEMParser(new FileReader(keyPath))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            if (object instanceof PEMKeyPair pemKeyPair) {
                KeyPair keyPair = converter.getKeyPair(pemKeyPair);
                return keyPair.getPrivate();
            } else if (object instanceof PrivateKeyInfo privateKeyInfo) {
                return converter.getPrivateKey(privateKeyInfo);
            } else {
                throw new RuntimeException("Unexpected PEM content");
            }
        }
    }

    private static void disableHostnameVerification(SSLEngine engine) {
        SSLParameters params = engine.getSSLParameters();
        params.setEndpointIdentificationAlgorithm(null);
        engine.setSSLParameters(params);
    }

    /**
     * Builds the FDO-shaped PID record for every resource type except Organisation, which has no
     * resourceOwner/type/publishingDate/urls of its own and is handled by {@link #createOrganisationPID}.
     */
    private String createPID(String pid, PidIssuerConfig config, LinkedHashMap<String, Object> payload,
                             List<String> resolveEndpoints, ResourceProperties.Fdo fdo) {
        JSONArray values = new JSONArray();
        int index = 1;

        for (Map<String, Object> alternativePid : asMapList(payload.get("alternativePIDs"))) {
            values.put(buildEntry(index++, "alternativePID", new JSONObject(alternativePid).toString()));
        }

        //TODO: what to do with resolve endpoints
        if (resolveEndpoints != null) {
            for (String endpoint : resolveEndpoints) {
                values.put(buildEntry(index++, "url", resolveUrl(endpoint, pid)));
            }
        }
        for (String url : asStringList(payload.get("urls"))) {
            values.put(buildEntry(index++, "url", url));
        }

        values.put(buildEntry(index++, "name", payload.get("name")));
        values.put(buildEntry(index++, "description", payload.get("description")));
        values.put(buildEntry(index++, "publishingDate", payload.get("publishingDate")));
        values.put(buildEntry(index++, "type", payload.get("type")));
        values.put(buildEntry(index++, "nodePID", payload.get("nodePID")));
        values.put(buildEntry(index++, "resourceOwner", payload.get("resourceOwner")));

        for (String contact : asStringList(payload.get("publicContacts"))) {
            values.put(buildEntry(index++, "publicContact", contact));
        }

        values.put(buildHsAdmin(config));
        addFdoFields(values, fdo);

        JSONObject data = new JSONObject();
        data.put("values", values);
        return data.toString();
    }

    /**
     * Organisations are their own resourceOwner and have no type/publishingDate/urls, so their
     * record is limited to the fields the Organisation model shares with the other resource types.
     */
    private String createOrganisationPID(String pid, PidIssuerConfig config, LinkedHashMap<String, Object> payload,
                                         List<String> resolveEndpoints, ResourceProperties.Fdo fdo) {
        JSONArray values = new JSONArray();
        int index = 1;

        for (Map<String, Object> alternativePid : asMapList(payload.get("alternativePIDs"))) {
            values.put(buildEntry(index++, "alternativePIDs", new JSONObject(alternativePid).toString()));
        }

        if (resolveEndpoints != null) {
            for (String endpoint : resolveEndpoints) {
                values.put(buildEntry(index++, "URL", resolveUrl(endpoint, pid)));
            }
        }

        values.put(buildEntry(index++, "name", payload.get("name")));
        values.put(buildEntry(index++, "description", payload.get("description")));
        values.put(buildEntry(index++, "nodePID", payload.get("nodePID")));

        for (String contact : asStringList(payload.get("publicContacts"))) {
            values.put(buildEntry(index++, "publicContacts", contact));
        }

        values.put(buildHsAdmin(config));
        addFdoFields(values, fdo);

        JSONObject data = new JSONObject();
        data.put("values", values);
        return data.toString();
    }

    /**
     * Substitutes {@code pid} into a resolve-endpoint template. A {@code {encodedPid}} placeholder gets
     * the PID with its internal "/" percent-encoded (for endpoints that expect the whole PID as a single
     * path segment, e.g. a search API); a {@code {pid}} placeholder gets the raw PID; an endpoint with
     * neither placeholder has the raw PID appended as a new path segment, for backward compatibility.
     */
    private String resolveUrl(String endpoint, String pid) {
        if (endpoint.contains("{encodedPid}")) {
            return endpoint.replace("{encodedPid}", pid.replace("/", "%252F"));
        } else if (endpoint.contains("{pid}")) {
            return endpoint.replace("{pid}", pid);
        }
        return String.join("/", endpoint, pid);
    }

    private void addFdoFields(JSONArray values, ResourceProperties.Fdo fdo) {
        values.put(buildEntry(FDO_TYPE_INDEX, "FdoType", fdo.getType()));
        values.put(buildEntry(FDO_PROFILE_INDEX, "FdoProfile", fdo.getProfile()));
        values.put(buildEntry(FDO_DATA_INDEX, "FdoData", fdo.getData()));
        values.put(buildEntry(FDO_VERSION_INDEX, "FdoVersion", fdo.getVersion()));
    }

    private JSONObject buildHsAdmin(PidIssuerConfig config) {
        JSONObject hsAdminValue = new JSONObject();
        hsAdminValue.put("handle", config.getUser());
        hsAdminValue.put("index", Integer.parseInt(config.getUserIndex()));
        hsAdminValue.put("permissions", "011111110011");

        JSONObject hsAdminData = new JSONObject();
        hsAdminData.put("format", "admin");
        hsAdminData.put("value", hsAdminValue);

        return buildEntry(HS_ADMIN_INDEX, "HS_ADMIN", hsAdminData);
    }

    private JSONObject buildEntry(int index, String type, JSONObject data) {
        JSONObject entry = new JSONObject();
        entry.put("index", index);
        entry.put("type", type);
        entry.put("data", data);
        return entry;
    }

    private JSONObject buildEntry(int index, String type, Object value) {
        JSONObject data = new JSONObject();
        data.put("format", "string");
        data.put("value", value);
        return buildEntry(index, type, data);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asMapList(Object value) {
        if (value instanceof List<?> list) {
            return (List<Map<String, Object>>) (List<?>) list;
        }
        return List.of();
    }

    private List<String> asStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
        }
        return result;
    }

    private HttpHeaders createHeaders(PidIssuerConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (config.getAuth() != null &&
                !config.getAuth().getClientKey().isBlank() &&
                !config.getAuth().getClientCert().isBlank()) {
            headers.set("Authorization", "Handle clientCert=\"true\"");
        } else if (StringUtils.hasText(config.getPassword())) {
            headers.set("Authorization",
                    createBasicAuth(config.getUser(), config.getUserIndex(), config.getPassword())
            );
        }

        return headers;
    }

    private String createBasicAuth(String user, String userIndex, String password) {
        String username = userIndex + "%3A" + user;
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        return "Basic " + new String(encodedAuth, StandardCharsets.UTF_8);
    }

    private void exchange(String payload, HttpHeaders headers, PidIssuerConfig config, String pid,
                          WebClient webClient, HttpMethod method) {
        try {
            URI uri = URI.create(String.join("/", config.getUrl(), pid));
            WebClient.RequestBodySpec requestSpec = webClient
                    .method(method)
                    .uri(uri)
                    .headers(h -> h.addAll(headers));

            WebClient.RequestHeadersSpec<?> headersSpec = (payload != null && !payload.isEmpty())
                    ? requestSpec.bodyValue(payload)
                    : requestSpec;

            ResponseEntity<String> response = headersSpec
                    .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class))
                    .block();

            logInfo(response, pid, config.getUrl(), method);
        } catch (Exception e) {
            throw new RuntimeException("Error during PID " + method.name() + " request", e);
        }
    }

    private void logInfo(ResponseEntity<?> response, String pid, String endpoint, HttpMethod method) {
        if (response.getStatusCode() == HttpStatus.CREATED) {
            logger.info("Resource with ID '{}' has been posted on [{}]", pid, endpoint);
        } else if (response.getStatusCode() == HttpStatus.OK) {
            if (method == HttpMethod.PUT) {
                logger.info("Resource with ID '{}' has been updated on [{}]", pid, endpoint);
            } else {
                logger.info("Resource with ID '{}' has been deleted from [{}]", pid, endpoint);
            }
        } else {
            logger.error("Resource with ID '{}' could not be posted/updated/deleted : [{}]", pid, response.getBody());
        }
    }

    // TODO: can be used in PidController to fetch the body
    public Map<String, Object> getResource(String pid) {
        PidServiceResponse response = getPidServiceResponse(pid);
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        }
        return null;
    }

    public PidServiceResponse getPidServiceResponse(String pid) {
        String prefix = pid.split("/")[0];
        ResourceProperties resourceProperties = properties.getResourcePropertiesFromPrefix(prefix);
        PidIssuerConfig config = resourceProperties.getPidIssuer();

        WebClient webClient = createWebClient(config);
        HttpHeaders headers = createHeaders(config);

        return webClient
                .get()
                .uri(URI.create(String.join("/", config.getUrl(), pid)))
                .headers(h -> h.addAll(headers))
                .exchangeToMono(response ->
                        response.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                                })
                                .map(body -> new PidServiceResponse(response.statusCode(), body))
                )
                .block();
    }
}
