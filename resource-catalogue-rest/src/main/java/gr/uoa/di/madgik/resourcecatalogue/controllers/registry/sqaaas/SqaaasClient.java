package gr.uoa.di.madgik.resourcecatalogue.controllers.registry.sqaaas;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Component
public class SqaaasClient {

    private final WebClient webClient;

    public SqaaasClient(
            @Value("${sqaaas.base-url}") String baseUrl
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024)) // 16MB buffer
                .build();
    }

    private JsonNode post(String path, Object body) {
        return webClient.post()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.err.println("SQAaaS POST " + path + " failed");
                                    System.err.println("Status: " + response.statusCode());
                                    System.err.println("Body: " + errorBody);
                                    return reactor.core.publisher.Mono.error(
                                            new RuntimeException("SQAaaS error: " + errorBody)
                                    );
                                })
                )
                .bodyToMono(JsonNode.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .block();
    }


    private JsonNode get(String path) {
        try {
            return webClient.get()
                    .uri(path)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            System.err.println("SQAaaS GET " + path + " failed:");
            System.err.println("Status: " + e.getStatusCode());
            System.err.println("Body: " + e.getResponseBodyAsString());
            throw e;
        }
    }

    public String createPipeline(Map<String, Object> payload) {
        return post("/pipeline/assessment", payload)
                .get("id").asText();
    }

    public void runPipeline(String pipelineId) {
        webClient.post()
                .uri("/pipeline/" + pipelineId + "/run")
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.err.println("Failed to run pipeline " + pipelineId);
                                    System.err.println("Status: " + response.statusCode());
                                    System.err.println("Body: " + errorBody);
                                    return reactor.core.publisher.Mono.error(
                                            new RuntimeException("Run failed: " + errorBody)
                                    );
                                })
                )
                .toBodilessEntity()
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .block();
    }

    public JsonNode getStatus(String pipelineId) {
        return get("/pipeline/" + pipelineId + "/status");
    }

    public JsonNode getOutput(String pipelineId) {
        return get("/pipeline/assessment/" + pipelineId + "/output");
    }
}
