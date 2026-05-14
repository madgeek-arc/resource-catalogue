package gr.uoa.di.madgik.resourcecatalogue.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@Service
public class NodeResolver {

    private static final Logger logger = LoggerFactory.getLogger(NodeResolver.class);

    private String nodeRegistryUrl;
    private String nodeRegistryKey;

    private WebClient webClient;

    public NodeResolver(@Value("${node.registry.url}") String nodeRegistryUrl,
                        @Value("${node.registry.key}") String nodeRegistryKey) {
        this.webClient = WebClient.builder()
                .baseUrl(nodeRegistryUrl)
                .build();
        this.nodeRegistryKey = nodeRegistryKey;
    }

    public record CapabilitiesResponse(
            @JsonProperty("node_endpoint")
            String nodeEndpoint,
            List<Capability> capabilities
    ) {
    }

    public record Capability(
            @JsonProperty("capability_type")
            String capabilityType,
            URI endpoint,
            String version
    ) {

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
        List<Node> nodes = (List<Node>) webClient.get()
                .header("x-api-key", nodeRegistryKey)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Node>>() {})
                .block();
        if (nodes == null) {
            return List.of();
        }

        return nodes.stream()
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
            CapabilitiesResponse response = webClient.mutate()
                    .baseUrl(UriComponentsBuilder.fromUri(nodeEndpoint).build().toUriString())
                    .build()
                    .get()
                    .retrieve()
                    .bodyToMono(CapabilitiesResponse.class)
                    .block();

            if (response == null || response.capabilities() == null) {
                return List.of();
            }

            return response.capabilities();
        } catch (Exception e) {
            logger.warn("Failed to fetch capabilities from node endpoint {}", nodeEndpoint, e);
            return Collections.emptyList();
        }
    }
}
