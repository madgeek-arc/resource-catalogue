package gr.uoa.di.madgik.resourcecatalogue.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiable;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import io.camunda.client.CamundaClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
class TestController {

    private final CamundaClient client;
    private final ObjectMapper mapper;

    public TestController(CamundaClient client,
                          ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @GetMapping("/providers/resource/{id}/{suffix}")
    public Map<String,Object> getResource(@PathVariable String id, @PathVariable String suffix) {
        var pid = "%s/%s".formatted(id, suffix);
        var key = client.newCreateInstanceCommand()
                .bpmnProcessId("reject-service")
                .latestVersion()
                .variables(Map.of("id", pid))
                .withResult()
                .requestTimeout(java.time.Duration.ofSeconds(35))
                .send()
                .join();
        return Map.of("instanceKey", key);
    }

    @PostMapping("/providers/onboard-async")
    public Map<String,Object> onboardAsync(@RequestBody ProviderBundle bundle) {
        Map<String, Object> provider = toBundleMap(bundle);
        var key = client.newCreateInstanceCommand()
                .bpmnProcessId("provider-onboarding")
                .latestVersion()
                .variables(provider)
                .send().join().getProcessInstanceKey();
        return Map.of("instanceKey", key);
    }

    @PostMapping("/providers/onboard")
    public Map<String,Object> onboard(@RequestBody ProviderBundle bundle) {
        Map<String, Object> provider = toBundleMap(bundle);
        var key = client.newCreateInstanceCommand()
                .bpmnProcessId("provider-onboarding")
                .latestVersion()
                .variables(provider)
                .withResult()
                .requestTimeout(java.time.Duration.ofSeconds(120))
                .send()
                .join();

        return Map.of("instance", key);
    }

    public Bundle toBundle(Map<String, Object> resource) {
        Bundle bundle = mapper.convertValue(resource, new TypeReference<>() {});
        // Adds the payload (because Jackson is set to ignore it)
        bundle.setPayload(mapper.convertValue(resource.get("payload"), new TypeReference<>() {}));
        return bundle;
    }

    public Map<String, Object> toMap(Bundle resource) {
        Bundle bundle = mapper.convertValue(resource, new TypeReference<>() {});
        bundle.setPayload(resource.getPayload());
        Map<String, Object> vars = mapper.convertValue(bundle, new TypeReference<Map<String, Object>>() {});

        // Adds the payload (because Jackson is set to ignore it)
        vars.put("payload", bundle.getPayload());

        return vars;
    }

    public <T extends Bundle> Map<String, Object> toBundleMap(T resource) {
        T bundle = mapper.convertValue(resource, new TypeReference<>() {});
        bundle.setPayload(resource.getPayload());
        Map<String, Object> vars = mapper.convertValue(bundle, new TypeReference<Map<String, Object>>() {});

        // Adds the payload (because Jackson is set to ignore it)
        vars.put("payload", bundle.getPayload());

        return vars;
    }
}
