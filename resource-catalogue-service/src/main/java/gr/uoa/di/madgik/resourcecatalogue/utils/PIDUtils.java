package gr.uoa.di.madgik.resourcecatalogue.utils;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Component
public class PIDUtils {

    private static final Logger logger = LoggerFactory.getLogger(PIDUtils.class);

    private String endpoint;
    private String user;
    private String userIndex;
    private String certPath;
    private String keyPath;

    // test
    @Value("${pid.test}")
    private boolean pidTest;
    @Value("${pid.test.user}")
    private String testUser;
    @Value("${pid.test.endpoint}")
    private String testEndpoint;
    @Value("${pid.test.auth}")
    private String testAuth;

    // MP
    @Value("${marketplace.endpoint}")
    private String marketplaceEndpoint;
    @Value("${marketplace.endpoint.enabled}")
    private boolean marketplaceEnabled;

    // providers
    @Value("${pid.providers.base-url}")
    private String providersEndpoint;
    @Value("${pid.providers.prefix}")
    private String providersPrefix;
    @Value("${pid.providers.auth.user}")
    private String providersUser;
    @Value("${pid.providers.auth.user.index}")
    private String providersUserIndex;
    @Value("${pid.providers.auth.client-cert}")
    private String providersCert;
    @Value("${pid.providers.auth.client-key}")
    private String providersKey;

    // services
    @Value("${pid.services.base-url}")
    private String servicesEndpoint;
    @Value("${pid.services.prefix}")
    private String servicesPrefix;
    @Value("${pid.services.auth.user}")
    private String servicesUser;
    @Value("${pid.services.auth.user.index}")
    private String servicesUserIndex;
    @Value("${pid.services.auth.client-cert}")
    private String servicesCert;
    @Value("${pid.services.auth.client-key}")
    private String servicesKey;

    // trainings
    @Value("${pid.trainings.base-url}")
    private String trainingsEndpoint;
    @Value("${pid.trainings.prefix}")
    private String trainingsPrefix;
    @Value("${pid.trainings.auth.user}")
    private String trainingsUser;
    @Value("${pid.trainings.auth.user.index}")
    private String trainingsUserIndex;
    @Value("${pid.trainings.auth.client-cert}")
    private String trainingsCert;
    @Value("${pid.trainings.auth.client-key}")
    private String trainingsKey;

    // guidelines
    @Value("${pid.interoperability-frameworks.base-url}")
    private String guidelinesEndpoint;
    @Value("${pid.interoperability-frameworks.prefix}")
    private String guidelinesPrefix;
    @Value("${pid.interoperability-frameworks.auth.user}")
    private String guidelinesUser;
    @Value("${pid.interoperability-frameworks.auth.user.index}")
    private String guidelinesUserIndex;
    @Value("${pid.interoperability-frameworks.auth.client-cert}")
    private String guidelinesCert;
    @Value("${pid.interoperability-frameworks.auth.client-key}")
    private String guidelinesKey;

    // tools
    @Value("${pid.tools.base-url}")
    private String toolsEndpoint;
    @Value("${pid.tools.prefix}")
    private String toolsPrefix;
    @Value("${pid.tools.auth.user}")
    private String toolsUser;
    @Value("${pid.tools.auth.user.index}")
    private String toolsUserIndex;
    @Value("${pid.tools.auth.client-cert}")
    private String toolsCert;
    @Value("${pid.tools.auth.client-key}")
    private String toolsKey;

    public void postPID(String pid) {
        RestTemplate restTemplate = setConfigurationSettings(pid);
        String payload = createPID(pid);
        HttpHeaders headers = createHeaders();

        HttpEntity<String> request = new HttpEntity<>(payload, headers);
        try {
            restTemplate.exchange(endpoint, HttpMethod.PUT, request, String.class);
            logger.info("Resource with ID [{}] has been posted with PID [{}] on [{}]", pid, pid, endpoint);
        } catch (Exception e) {
            throw new RuntimeException("Error during PID post request", e);
        }
    }

    private RestTemplate setConfigurationSettings(String pid) {
        String prefix = pid.split("/")[0];
        String suffix = pid.split("/")[1];
        RestTemplate restTemplate;
        if (pidTest) {
            endpoint = testEndpoint + suffix;
            restTemplate = RestTemplateTrustManager.createRestTemplateWithDisabledSSL();
        } else {
            if (prefix.equals(providersPrefix)) {
                endpoint = providersEndpoint + "api/handles/" + pid;
                user = providersUser;
                userIndex = providersUserIndex;
                certPath = providersCert;
                keyPath = providersKey;
            } else if (prefix.equals(servicesPrefix)) {
                endpoint = servicesEndpoint + "api/handles/" + pid;
                user = servicesUser;
                userIndex = servicesUserIndex;
                certPath = servicesCert;
                keyPath = servicesKey;
            } else if (prefix.equals(trainingsPrefix)) {
                endpoint = trainingsEndpoint + "api/handles/" + pid;
                user = trainingsUser;
                userIndex = trainingsUserIndex;
                certPath = trainingsCert;
                keyPath = trainingsKey;
            } else if (prefix.equals(guidelinesPrefix)) {
                endpoint = guidelinesEndpoint + "api/handles/" + pid;
                user = guidelinesUser;
                userIndex = guidelinesUserIndex;
                certPath = guidelinesCert;
                keyPath = guidelinesKey;
            } else if (prefix.equals(toolsPrefix)) {
                endpoint = toolsEndpoint + "api/handles/" + pid;
                user = toolsUser;
                userIndex = toolsUserIndex;
                certPath = toolsCert;
                keyPath = toolsKey;
            } else {
                throw new RuntimeException("Unknown prefix: " + prefix);
            }
            restTemplate = createSslRestTemplate(certPath, keyPath);
        }
        return restTemplate;
    }

    public RestTemplate createSslRestTemplate(String certPath, String keyPath) {
        try {
            // Load certificate
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate;
            try (FileReader certReader = new FileReader(certPath)) {
                certificate = (X509Certificate) certificateFactory.generateCertificate(new FileInputStream(new File(certPath)));
            }

            // Load private key
            PrivateKey privateKey;
            try (PEMParser pemParser = new PEMParser(new FileReader(keyPath))) {
                Object object = pemParser.readObject();
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                if (object instanceof PEMKeyPair) {
                    KeyPair keyPair = converter.getKeyPair((PEMKeyPair) object);
                    privateKey = keyPair.getPrivate();
                } else if (object instanceof PrivateKeyInfo) {
                    privateKey = converter.getPrivateKey((PrivateKeyInfo) object);
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

            // Create HttpClient with the custom SSL context
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();

            // Set the HttpClient on the RestTemplate
            return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));

        } catch (Exception e) {
            throw new RuntimeException("Error configuring RestTemplate with PEM files", e);
        }
    }

    private String createPID(String pid) {
        JSONObject data = new JSONObject();
        JSONArray values = new JSONArray();
        JSONObject hs_admin = new JSONObject();
        JSONObject hs_admin_data = new JSONObject();
        JSONObject hs_admin_data_value = new JSONObject();
        JSONObject id = new JSONObject();
        JSONObject id_data = new JSONObject();
        JSONObject marketplaceUrl = new JSONObject();
        JSONObject marketplaceUrl_data = new JSONObject();
        if (pidTest) {
            hs_admin_data_value.put("handle", testUser);
            hs_admin_data_value.put("index", 301);
        } else {
            hs_admin_data_value.put("handle", user);
            hs_admin_data_value.put("index", userIndex);
        }
        hs_admin_data_value.put("permissions", "011111110011");
        hs_admin_data.put("format", "admin");
        hs_admin_data.put("value", hs_admin_data_value);
        hs_admin.put("index", 100);
        hs_admin.put("type", "HS_ADMIN");
        hs_admin.put("data", hs_admin_data);
        values.put(hs_admin);
        id_data.put("format", "string");
        id_data.put("value", pid);
        id.put("index", 12345);
        id.put("type", "testing update");
        id.put("data", id_data);
        values.put(id);
        //TODO: Enable when we have final MP endpoints (which projects?)
        if (marketplaceEnabled) {
            String url = marketplaceEndpoint + determineMarketplaceEndpoint(pid.split("/")[0]);
            marketplaceUrl_data.put("format", "string");
            marketplaceUrl_data.put("value", url + pid);
            marketplaceUrl.put("index", 2);
            marketplaceUrl.put("type", "url");
            marketplaceUrl.put("data", marketplaceUrl_data);
            values.put(marketplaceUrl);
        }
        data.put("values", values);
        return data.toString();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (pidTest) {
            headers.set("Authorization", testAuth);
        } else {
            headers.set("Authorization", "Handle clientCert=\"true\"");
        }
        return headers;
    }

    //TODO: Update with new URL paths
    public String determineMarketplaceEndpoint(String prefix) {
        if (prefix.equals(providersPrefix)) {
            return "providers/";
        } else if (prefix.equals(servicesPrefix)) {
            return "services/";
        } else if (prefix.equals(trainingsPrefix)) {
            return "trainings/";
        } else if (prefix.equals(guidelinesPrefix)) {
            return "guidelines/";
        } else {
            return "tools/";
        }
    }

    public String determineResourceTypeFromPidPrefix(String prefix) {
        if (prefix.equals(servicesPrefix)) {
            return "service";
        } else if (prefix.equals(toolsPrefix)) {
            return "tool";
        } else if (prefix.equals(trainingsPrefix)) {
            return "training_resource";
        } else if (prefix.equals(providersPrefix)) {
            return "provider";
        } else if (prefix.equals(guidelinesPrefix)) {
            return "interoperability_record";
        } else {
            return "no_resource_type";
        }
    }
}
