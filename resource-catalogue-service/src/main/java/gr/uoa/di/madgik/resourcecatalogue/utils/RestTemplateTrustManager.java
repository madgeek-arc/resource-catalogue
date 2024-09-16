package gr.uoa.di.madgik.resourcecatalogue.utils;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import javax.net.ssl.*;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

public class RestTemplateTrustManager {

    public static RestTemplate createRestTemplateWithDisabledSSL() {
        try {
            SSLContext sslContext = SSLContextBuilder
                    .create()
                    .loadTrustMaterial((chain, authType) -> true) // Trust all certificates
                    .build();
            CloseableHttpClient httpClient = HttpClients
                    .custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
            HttpComponentsClientHttpRequestFactory requestFactory =
                    new HttpComponentsClientHttpRequestFactory(httpClient);

            return new RestTemplate(requestFactory);
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new RuntimeException("Failed to create RestTemplate with disabled SSL verification", e);
        }
    }
}
