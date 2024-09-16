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
import org.springframework.http.*;
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
    private String certPath;
    private String keyPath;

    @Value("${pid.test}")
    private boolean pidTest;
    @Value("${pid.test.user}")
    private String testUser;
    @Value("${pid.test.endpoint}")
    private String testEndpoint;
    @Value("${pid.test.auth}")
    private String testAuth;

    @Value("${pid.user.prefix}")
    private String userPrefix;
    @Value("${pid.user.suffix}")
    private String userSuffix;

    @Value("${pid.endpoint.providers}")
    private String providersEndpoint;
    @Value("${pid.endpoint.services}")
    private String servicesEndpoint;
    @Value("${pid.endpoint.trainings}")
    private String trainingsEndpoint;
    @Value("${pid.endpoint.guidelines}")
    private String guidelinesEndpoint;
    @Value("${pid.endpoint.tools}")
    private String toolsEndpoint;

    @Value("${marketplace.endpoint.enabled}")
    private boolean marketplaceEnabled;
    @Value("${marketplace.endpoint}")
    private String marketplaceEndpoint;

    @Value("${pid.cert.providers}")
    private String providersCert;
    @Value("${pid.cert.services}")
    private String servicesCert;
    @Value("${pid.cert.trainings}")
    private String trainingsCert;
    @Value("${pid.cert.guidelines}")
    private String guidelinesCert;
    @Value("${pid.cert.tools}")
    private String toolsCert;

    @Value("${pid.key.providers}")
    private String providersKey;
    @Value("${pid.key.services}")
    private String servicesKey;
    @Value("${pid.key.trainings}")
    private String trainingsKey;
    @Value("${pid.key.guidelines}")
    private String guidelinesKey;
    @Value("${pid.key.tools}")
    private String toolsKey;

    @Value("${prefix.providers}")
    private String providersPrefix;
    @Value("${prefix.services}")
    private String servicesPrefix;
    @Value("${prefix.trainings}")
    private String trainingsPrefix;
    @Value("${prefix.guidelines}")
    private String guidelinesPrefix;
    @Value("${prefix.tools}")
    private String toolsPrefix;

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
                endpoint = providersEndpoint + pid;
                certPath = providersCert;
                keyPath = providersKey;
            } else if (prefix.equals(servicesPrefix)) {
                endpoint = servicesEndpoint + pid;
                certPath = servicesCert;
                keyPath = servicesKey;
            } else if (prefix.equals(trainingsPrefix)) {
                endpoint = trainingsEndpoint + pid;
                certPath = trainingsCert;
                keyPath = trainingsKey;
            } else if (prefix.equals(guidelinesPrefix)) {
                endpoint = guidelinesEndpoint + pid;
                certPath = guidelinesCert;
                keyPath = guidelinesKey;
            } else if (prefix.equals(toolsPrefix)) {
                endpoint = toolsEndpoint + pid;
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
            hs_admin_data_value.put("handle", pid.split("/")[0] + userSuffix);
            hs_admin_data_value.put("index", userPrefix.split(":")[0]);
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
        id.put("index", 1);
        id.put("type", "id");
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
