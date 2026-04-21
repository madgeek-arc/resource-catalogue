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
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class PidIssuer {

    private static final Logger logger = LoggerFactory.getLogger(PidIssuer.class);

    private final CatalogueProperties properties;

    public PidIssuer(CatalogueProperties properties) {
        this.properties = properties;
    }

    public void postPID(String pid, List<String> customResolveEndpoints) {
        sendPIDRequest(pid, customResolveEndpoints, false);
    }

    public void deletePID(String pid) {
        sendPIDRequest(pid, null, true);
    }

    private void sendPIDRequest(String pid, List<String> customResolveEndpoints, boolean delete) {
        String prefix = pid.split("/")[0];
        ResourceProperties resourceProperties = properties.getResourcePropertiesFromPrefix(prefix);
        PidIssuerConfig config = resourceProperties.getPidIssuer();
        WebClient webClient = createWebClient(config);
        HttpHeaders headers = createHeaders(config);
        if (!delete) {
            String payload;
            if (customResolveEndpoints != null && !customResolveEndpoints.isEmpty()) {
                payload = createPID(pid, config, customResolveEndpoints, true);
            } else {
                payload = createPID(pid, config, resourceProperties.getResolveEndpoints(), false);
            }
            exchange(payload, headers, config, pid, webClient, HttpMethod.PUT);
        } else {
            exchange(null, headers, config, pid, webClient, HttpMethod.DELETE);
        }
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

    private String createPID(String pid, PidIssuerConfig config, List<String> resolveEndpoints, boolean isCustom) {
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
