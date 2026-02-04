package gr.uoa.di.madgik.resourcecatalogue.onboarding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.CatalogueBundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.service.CatalogueService;
import io.camunda.client.CamundaClient;
import io.camunda.client.annotation.CustomHeaders;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.VariablesAsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@Component
public class OnboardingService <T extends Bundle> {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingService.class);

    private final CatalogueService catalogueService;
    private final GenericResourceService genericResourceService;
    private final ObjectMapper mapper;
    private final CamundaClient client;

    public OnboardingService(CatalogueService catalogueService,
                             GenericResourceService genericResourceService,
                             ObjectMapper objectMapper,
                             CamundaClient client) {
        this.catalogueService = catalogueService;
        this.genericResourceService = genericResourceService;
        this.mapper = objectMapper;
        this.client = client;
    }

    public T onboard(String resourceType, T bundle, Authentication authentication) {
        String bpmnProcess = getBpmnProcess(resourceType);
        Map<String, Object> vars = new HashMap<>();
        Class<T> clazz = (Class<T>) bundle.getClass();
        vars.put("resource", toMap(bundle));
        vars.put("user", UserInfo.of(authentication));
        vars.put("class", clazz);
        var key = client.newCreateInstanceCommand()
                .bpmnProcessId(bpmnProcess)
                .latestVersion()
                .variables(vars)
                .withResult()
                .requestTimeout(java.time.Duration.ofSeconds(120))
                .send()
                .join();
        bundle = toBundle((Map<String, Object>) key.getVariablesAsMap().get("resource"), clazz);
        return bundle;
    }

    private String getBpmnProcess(String resourceType) {
        // TODO: load bpmn process by resourceType
        return switch (resourceType) {
            case "provider" -> "onboard-provider";
            default -> "onboard-resource";
        };
    }

    @JobWorker(type = "resource-onboard", autoComplete = true)
    public Map<String, Object> onboardResource(@VariablesAsType Map<String, Object> vars,
                                               @CustomHeaders Map<String, String> headers) {
        String resourceName = headers.getOrDefault("resourceName", "resource");
        String status = headers.get("status");
        String active = headers.get("active");

        T bundle = getResource(vars);
        UserInfo user = toUser((Map<String, Object>) vars.get("user"));
        bundle.markOnboard(status, active.equalsIgnoreCase("true"), user, "change status job");

        logger.info("Running task 'resource-status.apply' for {} with id '{}' | status: {}", resourceName, bundle.getId(), status);

        return Map.of(resourceName, toMap(bundle));
    }

    @JobWorker(type = "get-resource", autoComplete = true)
    public Map<String, Object> getResource(@VariablesAsType Map<String, Object> vars,
                                 @CustomHeaders Map<String, String> headers) {
        String id = (String) vars.get("id");
        String resourceType = headers.getOrDefault("resourceType", "resourceTypes");
        String resourceName = headers.getOrDefault("resourceName", "resource");
        logger.info("Reading resource with resourceType '{}' and id '{}'", resourceType, id);
        T bundle = genericResourceService.get(resourceType, id);
        return Map.of(resourceName, toMap(bundle));
    }

    @JobWorker(type = "save-resource", autoComplete = true)
    public Map<String, Object> saveResource(@VariablesAsType Map<String, Object> vars,
                                 @CustomHeaders Map<String, String> headers) throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException {
        String resourceType = headers.getOrDefault("resourceType", "resourceTypes");
        String resourceName = headers.getOrDefault("resourceName", "resource");
        T resource = toBundle((Map<String,Object>) vars.get(resourceName));
        logger.info("Saving resource with resourceType '{}' and id '{}'", resourceType, resource.getId());
        T bundle = genericResourceService.update(resourceType, resource.getId(), resource);
        return Map.of(resourceName, bundle);
    }

    @JobWorker(type = "get-catalogue", autoComplete = true)
    public Map<String, Object> getCatalogue(@VariablesAsType Map<String, Object> vars) {
        var catalogueId = (String) vars.get("catalogueId");
        CatalogueBundle catalogue = null;
        try {
            catalogue = catalogueService.get(catalogueId);
        } catch (Exception ignore) {}
        vars.put("catalogue", catalogue);
        return vars;
    }

    public UserInfo toUser(Map<String, Object> user) {
        return mapper.convertValue(user, UserInfo.class);
    }

    public T getResource(Map<String, Object> vars) {
        Map<String, Object> resource = (Map<String, Object>) vars.get("resource");
        Class<T> clazz = mapper.convertValue(vars.get("class"), new TypeReference<>() {});
        T bundle = mapper.convertValue(resource, clazz);
        // Adds the payload (because Jackson is set to ignore it)
        bundle.setPayload(mapper.convertValue(resource.get("payload"), new TypeReference<>() {}));
        return bundle;
    }

    public T toBundle(Map<String, Object> resource) {
        T bundle = mapper.convertValue(resource, new TypeReference<>() {});
        // Adds the payload (because Jackson is set to ignore it)
        bundle.setPayload(mapper.convertValue(resource.get("payload"), new TypeReference<>() {}));
        return bundle;
    }

    public T toBundle(Map<String, Object> resource, Class<T> clazz) {
        T bundle = mapper.convertValue(resource, clazz);
        // Adds the payload (because Jackson is set to ignore it)
        bundle.setPayload(mapper.convertValue(resource.get("payload"), new TypeReference<>() {}));
        return bundle;
    }

    public Map<String, Object> toMap(T resource) {
        T bundle = mapper.convertValue(resource, new TypeReference<>() {});
        bundle.setPayload(resource.getPayload());
        Map<String, Object> vars = mapper.convertValue(bundle, new TypeReference<Map<String, Object>>() {});

        // Adds the payload (because Jackson is set to ignore it)
        vars.put("payload", bundle.getPayload());

        return vars;
    }
}
