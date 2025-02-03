package gr.uoa.di.madgik.resourcecatalogue.manager.pids;

import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.ResourceProperties;
import gr.uoa.di.madgik.resourcecatalogue.utils.RestTemplateTrustManager;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.utils.Base64;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

@Component
public class PidIssuer {

    private static final Logger logger = LoggerFactory.getLogger(PidIssuer.class);

    private final CatalogueProperties properties;

    public PidIssuer(CatalogueProperties properties) {
        this.properties = properties;
    }

    public void postPID(String pid) {
        String prefix = pid.split("/")[0];
        ResourceProperties resourceProperties = properties.getResourcePropertiesFromPrefix(prefix);
        PidIssuerConfig config = resourceProperties.getPidIssuer();
        RestTemplate restTemplate = createRestTemplate(config);
        String payload = createPID(pid, config, resourceProperties.getResolveEndpoints());
        HttpHeaders headers = createHeaders(config);

        exchange(payload, headers, config, pid, restTemplate);
    }

    //TODO: revision certs VS basic auth VS testing basic auth
    private RestTemplate createRestTemplate(PidIssuerConfig config) {
        RestTemplate restTemplate;
        if (config.getAuth() != null) {
            if (config.getAuth().isSelfSignedCert()) {
                restTemplate = RestTemplateTrustManager.createRestTemplateWithDisabledSSL();
            } else {
                restTemplate = createCertBasedRestTemplate(
                        config.getAuth().getClientCert(),
                        config.getAuth().getClientKey());
            }
        } else {
            restTemplate = new RestTemplate();
        }
        return restTemplate;
    }

    public RestTemplate createCertBasedRestTemplate(String certPath, String keyPath) {
        try {
            // Load certificate
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate;
            try (FileReader certReader = new FileReader(certPath)) {
                certificate = (X509Certificate) certificateFactory
                        .generateCertificate(new FileInputStream(new File(certPath)));
            }

            // Load private key
            PrivateKey privateKey;
            try (PEMParser pemParser = new PEMParser(new FileReader(keyPath))) {
                Object object = pemParser.readObject();
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                if (object instanceof PEMKeyPair pemKeyPair) {
                    KeyPair keyPair = converter.getKeyPair(pemKeyPair);
                    privateKey = keyPair.getPrivate();
                } else if (object instanceof PrivateKeyInfo privateKeyInfo) {
                    privateKey = converter.getPrivateKey(privateKeyInfo);
                } else {
                    throw new RuntimeException("Unexpected PEM content");
                }
            }

            // Build the KeyStore with the loaded private key and certificate
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);
            char[] emptyPassword = new char[0];
            keyStore.setKeyEntry("client", privateKey, emptyPassword, new X509Certificate[]{certificate});

            // Build the SSL context with the key store
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadKeyMaterial(keyStore, emptyPassword)
                    .build();

            // Create ConnectionManager with the custom SSL context
            PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(
                            SSLConnectionSocketFactoryBuilder.create()
                                    .setSslContext(sslContext)
                                    .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                    .build()
                    )
                    .build();

            // Create HttpClient
            CloseableHttpClient httpClient = HttpClients
                    .custom()
                    .setConnectionManager(connectionManager)
                    .build();

            // Set the HttpClient on the RestTemplate
            return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));

        } catch (Exception e) {
            throw new RuntimeException("Error configuring RestTemplate with PEM files", e);
        }
    }

    private String createPID(String pid, PidIssuerConfig config, List<String> resolveEndpoints) {
        JSONObject data = new JSONObject();
        JSONArray values = new JSONArray();
        JSONObject hs_admin = new JSONObject();
        JSONObject hs_admin_data = new JSONObject();
        JSONObject hs_admin_data_value = new JSONObject();
        JSONObject id = new JSONObject();
        JSONObject id_data = new JSONObject();

        hs_admin_data_value.put("handle", config.getUser());
        hs_admin_data_value.put("index", config.getUserIndex());
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
        id.put("type", "id");
        id.put("data", id_data);
        values.put(id);
        if (resolveEndpoints != null && !resolveEndpoints.isEmpty()) {
            int index = 2;
            for (String endpoint : resolveEndpoints) {
                JSONObject resolveUrls = new JSONObject();
                JSONObject resolveUrl_data = new JSONObject();
                resolveUrl_data.put("format", "string");
                resolveUrl_data.put("value", String.join("/", endpoint, pid));
                resolveUrls.put("index", index);
                resolveUrls.put("type", "url");
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
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes());
        return "Basic " + new String(encodedAuth);
    }

    private void exchange(String payload, HttpHeaders headers, PidIssuerConfig config, String pid,
                          RestTemplate restTemplate) {
        HttpEntity<String> request = new HttpEntity<>(payload, headers);
        try {
            URI uri = URI.create(String.join("/", config.getUrl(), pid));
            ResponseEntity<?> response = restTemplate.exchange(uri, HttpMethod.PUT, request, String.class);
            logInfo(response, pid, config.getUrl());
        } catch (Exception e) {
            throw new RuntimeException("Error during PID post request", e);
        }
    }

    private void logInfo(ResponseEntity<?> response, String pid, String endpoint) {
        if (response.getStatusCode() == HttpStatus.CREATED) {
            logger.info("Resource with ID [{}] has been posted on [{}]", pid, endpoint);
        } else if (response.getStatusCode() == HttpStatus.OK) {
            logger.info("Resource with ID [{}] has been updated on [{}]", pid, endpoint);
        } else {
            logger.error("Resource with ID [{}] could not be posted/updated : [{}]", pid, response.getBody());
        }
    }
}
