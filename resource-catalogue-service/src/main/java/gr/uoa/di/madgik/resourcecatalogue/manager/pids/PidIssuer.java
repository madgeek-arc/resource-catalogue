package gr.uoa.di.madgik.resourcecatalogue.manager.pids;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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
public class PidIssuer {

    private static final Logger logger = LoggerFactory.getLogger(PidIssuer.class);

    private final PidProperties pidProperties;

    public PidIssuer(PidProperties pidProperties) {
        this.pidProperties = pidProperties;
    }

    // MP
    @Value("${marketplace.endpoint:}")
    private String marketplaceEndpoint;
    @Value("${marketplace.endpoint.enabled:false}")
    private boolean marketplaceEnabled;

    // providers
    @Value("${prefix.providers}")
    private String providersPrefix;

    // services
    @Value("${prefix.services}")
    private String servicesPrefix;

    // trainings
    @Value("${prefix.trainings}")
    private String trainingsPrefix;

    // guidelines
    @Value("${prefix.interoperability-frameworks}")
    private String guidelinesPrefix;

    // tools
    @Value("${prefix.tools}")
    private String toolsPrefix;



    public void postPID(String pid) {
        String prefix = pid.split("/")[0];
        PidIssuerConfig config = pidProperties.getIssuerConfigurationByResource(prefix);
        RestTemplate restTemplate = createRestTemplate(config);
        String payload = createPID(pid, config);
        HttpHeaders headers = createHeaders(config);

        HttpEntity<String> request = new HttpEntity<>(payload, headers);
        try {
            restTemplate.exchange(config.getUrl(), HttpMethod.PUT, request, String.class);
            logger.info("Resource with ID [{}] has been posted with PID [{}] on [{}]", pid, pid, config.getUrl());
        } catch (Exception e) {
            throw new RuntimeException("Error during PID post request", e);
        }
    }

    private RestTemplate createRestTemplate(PidIssuerConfig config) {
        RestTemplate restTemplate;
        if (!config.getUrl().startsWith("https")) {
//            config.setBaseUrl(testEndpoint + suffix);
            restTemplate = RestTemplateTrustManager.createRestTemplateWithDisabledSSL();
        } else if (config.getAuth() != null) {
            restTemplate = createSslRestTemplate(config.getAuth().getClientCert(), config.getAuth().getClientKey());
        } else {
            restTemplate = new RestTemplate();
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

//    private RestTemplate createBasicAuthRestTemplate(String username, String password) {
//        // Set up credentials provider
//        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//        Credentials credentials = new UsernamePasswordCredentials(username, password.toCharArray());
//        credentialsProvider.setCredentials(new AuthScope(null, -1), credentials);
//
//        // Build HttpClient with Basic Auth
//        CloseableHttpClient httpClient = HttpClients.custom()
//                .setDefaultCredentialsProvider(credentialsProvider)
//                .build();
//
//        // Create RestTemplate with HttpComponentsClientHttpRequestFactory
//        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
//    }

    private String createPID(String pid, PidIssuerConfig config) {
        JSONObject data = new JSONObject();
        JSONArray values = new JSONArray();
        JSONObject hs_admin = new JSONObject();
        JSONObject hs_admin_data = new JSONObject();
        JSONObject hs_admin_data_value = new JSONObject();
        JSONObject id = new JSONObject();
        JSONObject id_data = new JSONObject();
        JSONObject marketplaceUrl = new JSONObject();
        JSONObject marketplaceUrl_data = new JSONObject();

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

    private HttpHeaders createHeaders(PidIssuerConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (config.getAuth() != null) {
            headers.set("Authorization", "Handle clientCert=\"true\"");
        } else if (StringUtils.hasText(config.getPassword())) {
            headers.set("Authorization", createBasicAuth(config.getUser(), config.getPassword()));
        }

        return headers;
    }

    private String createBasicAuth(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes());
        return "Basic " + new String(encodedAuth);
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
