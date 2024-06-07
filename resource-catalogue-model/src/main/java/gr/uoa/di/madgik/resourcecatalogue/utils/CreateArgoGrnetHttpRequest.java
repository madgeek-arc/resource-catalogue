package gr.uoa.di.madgik.resourcecatalogue.utils;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class CreateArgoGrnetHttpRequest {

    public static String createHttpRequest(String url, String token) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("accept", "application/json");
        headers.add("Content-Type", "application/json");
        headers.add("x-api-key", token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
        } catch (HttpClientErrorException e) {
            return null;
        }
    }

    private CreateArgoGrnetHttpRequest() {
    }
}
