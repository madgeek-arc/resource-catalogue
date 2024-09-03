package gr.uoa.di.madgik.resourcecatalogue.utils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Component
public class PIDUtils {

    private static final Logger logger = LoggerFactory.getLogger(PIDUtils.class);

    private String endpoint;
    private String certPath;
    private String keyPath;

    @Value("${truststore.path}")
    private String truststorePath;
    @Value("${truststore.pass}")
    private String truststorePass;

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

    @Value("${pid.endpoint.base}")
    private String baseEndpoint;
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
        setConfigurationSettings(pid);
        String payload = createPID(pid);
        HttpURLConnection con = getHttpURLConnection(payload, endpoint);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            logger.info("Resource with ID [{}] has been posted with PID [{}] on [{}]", pid, pid, endpoint);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setConfigurationSettings(String pid) {
        String prefix = pid.split("/")[0];
        String suffix = pid.split("/")[1];
        if (pidTest) {
            disableSSLVerification();
            endpoint = testEndpoint + suffix;
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
        hs_admin_data_value.put("handle", pid.split("/")[0] + userSuffix);
        hs_admin_data_value.put("index", userPrefix.split(":")[0]);
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

    private HttpURLConnection getHttpURLConnection(String payload, String endpoint) {
        HttpURLConnection con;
        try {
            con = (HttpURLConnection) new URL(endpoint).openConnection();
        } catch (IOException e) {
            throw new RuntimeException("Error configuring the HTTP connection", e);
        }
        try {
            con.setRequestMethod("PUT");
        } catch (ProtocolException e) {
            throw new RuntimeException("Error configuring the HTTP connection's protocol", e);
        }
        if (pidTest) {
            con.setRequestProperty("Authorization", testAuth);
        } else {
            con.setRequestProperty("Authorization", "Handle clientCert=\"true\"");
            configureSSL(con);
        }
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = payload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        } catch (IOException e) {
            throw new RuntimeException("Error configuring connection's output stream", e);
        }
        return con;
    }

    private void configureSSL(HttpURLConnection con) {
        try {
            // Load the private key from the PEM file
            PrivateKey privateKey = loadPrivateKey(keyPath);

            // Load the certificate from the PEM file
            X509Certificate certificate = loadCertificate(certPath);

            // Create a KeyStore to hold the private key and certificate
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setKeyEntry("client", privateKey, null, new Certificate[]{certificate});

            // Initialize KeyManagerFactory with the KeyStore containing client certificates
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, null);

            // Load the custom trust store
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (InputStream trustStoreStream = new FileInputStream(truststorePath)) {
                trustStore.load(trustStoreStream, truststorePass.toCharArray());
            }

            // Initialize TrustManagerFactory with the default CA certificates (system default trust store)
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore); // Initializes with the default trust store

            // Create SSL context with key and trust managers
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new java.security.SecureRandom());

            // Set the SSLSocketFactory on the HttpsURLConnection
            ((HttpsURLConnection) con).setSSLSocketFactory(sslContext.getSocketFactory());

        } catch (Exception e) {
            throw new RuntimeException("Error configuring SSL context", e);
        }
    }

    private PrivateKey loadPrivateKey(String filePath) throws Exception {
        String privateKeyPem = new String(Files.readAllBytes(Paths.get(filePath)));
        privateKeyPem = privateKeyPem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(privateKeyPem);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    private X509Certificate loadCertificate(String filePath) throws Exception {
        try (InputStream inStream = new FileInputStream(filePath)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(inStream);
        }
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

    private static void disableSSLVerification() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
            throw new RuntimeException("Failed to disable SSL verification", e);
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
