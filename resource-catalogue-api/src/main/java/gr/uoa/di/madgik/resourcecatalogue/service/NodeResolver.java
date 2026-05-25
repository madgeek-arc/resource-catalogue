package gr.uoa.di.madgik.resourcecatalogue.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import gr.uoa.di.madgik.node.capabilities.model.Capability;
import gr.uoa.di.madgik.node.capabilities.model.NodeCapabilities;
import gr.uoa.di.madgik.node.endpoint.client.HttpNodeCapabilitiesClient;
import gr.uoa.di.madgik.node.endpoint.client.NodeClientException;
import gr.uoa.di.madgik.resourcecatalogue.config.NodeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
public class NodeResolver {

    private static final Logger logger = LoggerFactory.getLogger(NodeResolver.class);
    private static final Duration CAPABILITIES_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration CAPABILITIES_REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_LOGGED_RESPONSE_BODY_LENGTH = 500;

    private final String nodeRegistryKey;

    private final WebClient webClient;
    private final HttpClient capabilitiesHttpClient;
    private final ObjectMapper objectMapper;

    public NodeResolver(NodeProperties nodeProperties, ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .baseUrl(nodeProperties.getRegistry().getUrl())
                .build();
        this.capabilitiesHttpClient = HttpClient.newBuilder()
                .connectTimeout(CAPABILITIES_CONNECT_TIMEOUT)
                .build();
        this.nodeRegistryKey = nodeProperties.getRegistry().getKey();
        this.objectMapper = objectMapper;
    }

    public record LegalEntity(String name, @JsonProperty("ror_id") String rorId) {}

    public record Node(
            String id,
            String name,
            URI logo,
            String pid,
            @JsonProperty("legal_entity")
            LegalEntity legalEntity,
            @JsonProperty("node_endpoint")
            URI nodeEndpoint,
            List<Capability> capabilities) {
    }

    @Cacheable(cacheNames = "nodes", unless = "#result == null || #result.isEmpty()")
    public List<Node> fetchNodes() {
        List<Node> nodes = webClient.get()
                .header("x-api-key", nodeRegistryKey)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Node>>() {})
                .block();
        if (nodes == null) {
            return List.of();
        }

        return nodes.parallelStream()
                .map(this::populateCapabilities)
                .toList();
    }

    private Node populateCapabilities(Node node) {
        List<Capability> capabilities = fetchCapabilities(node.nodeEndpoint());
        return new Node(
                node.id(),
                node.name(),
                node.logo(),
                node.pid(),
                node.legalEntity(),
                node.nodeEndpoint(),
                capabilities
        );
    }

    private List<Capability> fetchCapabilities(URI nodeEndpoint) {
        try {
            NodeCapabilities response = HttpNodeCapabilitiesClient.builder(nodeEndpoint)
                    .httpClient(capabilitiesHttpClient)
                    .objectMapper(objectMapper)
                    .requestTimeout(CAPABILITIES_REQUEST_TIMEOUT)
                    .build()
                    .get();

            if (response == null || response.getCapabilities() == null) {
                return List.of();
            }

            return response.getCapabilities();
        } catch (NodeClientException e) {
            logNodeClientException(nodeEndpoint, e);
            return Collections.emptyList();
        } catch (Exception e) {
            logger.warn("Failed to fetch capabilities from node endpoint {}", nodeEndpoint, e);
            return Collections.emptyList();
        }
    }

    private void logNodeClientException(URI nodeEndpoint, NodeClientException e) {
        if (e.getStatusCode() > 0) {
            logger.warn("Failed to fetch capabilities from node endpoint {}: HTTP {}, body={}",
                    nodeEndpoint, e.getStatusCode(), truncate(e.getResponseBody()));
        } else {
            logger.warn("Failed to fetch capabilities from node endpoint {}: {}",
                    nodeEndpoint, e.getMessage());
        }
        logger.debug("Node endpoint capabilities fetch failed", e);
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_LOGGED_RESPONSE_BODY_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_LOGGED_RESPONSE_BODY_LENGTH) + "...";
    }
}
