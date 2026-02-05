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
public class WorkflowService<T extends Bundle> {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowService.class);

    private final CatalogueService catalogueService;
    private final GenericResourceService genericResourceService;
    private final ObjectMapper mapper;
    private final CamundaClient client;

    public WorkflowService(CatalogueService catalogueService,
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
        putResourceBundle(vars, bundle);
        putUserInfo(vars, UserInfo.of(authentication));
        var key = client.newCreateInstanceCommand()
                .bpmnProcessId(bpmnProcess)
                .latestVersion()
                .variables(vars)
                .withResult()
                .requestTimeout(java.time.Duration.ofSeconds(120))
                .send()
                .join();
        bundle = getResourceBundle(key.getVariablesAsMap());
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
        String comment = headers.getOrDefault("comment", "");

        T bundle = getResourceBundle(vars);
        UserInfo user = getUserInfo(vars);
        bundle.markOnboard(status, active.equalsIgnoreCase("true"), user, comment);

        logger.info("Running task 'resource-status.apply' for {} with id '{}' | status: {}", resourceName, bundle.getId(), status);

        return Map.of(resourceName, toMap(bundle));
    }

    @JobWorker(type = "get-resource", autoComplete = true)
    public Map<String, Object> getResourceBundle(@VariablesAsType Map<String, Object> vars,
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
        T resource = getResourceBundle(vars, resourceName);
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

    public T getResourceBundle(Map<String, Object> vars) {
        return getResourceBundle(vars, "resource");
    }

    public void putResourceBundle(Map<String, Object> vars, T bundle) {
        putResourceBundle(vars, bundle, "resource");
    }

    public T getResourceBundle(Map<String, Object> vars, String resourceName) {
        String classKey = resourceName + "_class";
        Map<String, Object> resource = (Map<String, Object>) vars.get(resourceName);
        T bundle;
        if (vars.containsKey(classKey)) {
            Class<T> clazz = mapper.convertValue(vars.get(classKey), new TypeReference<>() {});
            bundle = mapper.convertValue(resource, clazz);
        } else {
            bundle = mapper.convertValue(resource, new TypeReference<>() {});
        }
        // Adds the payload (because Jackson is set to ignore it)
        bundle.setPayload(mapper.convertValue(resource.get("payload"), new TypeReference<>() {}));
        return bundle;
    }

    public void putResourceBundle(Map<String, Object> vars, T bundle, String resourceName) {
        Class<T> clazz = (Class<T>) bundle.getClass();
        vars.put(resourceName + "_class", clazz);
        vars.put(resourceName, toMap(bundle));
    }

    public UserInfo getUserInfo(Map<String, Object> vars) {
        return mapper.convertValue(vars.get("user"), UserInfo.class);
    }

    public void putUserInfo(Map<String, Object> vars, UserInfo user) {
        vars.put("user", user);
    }

    private Map<String, Object> toMap(T resource) {
        Class<T> clazz = (Class<T>) resource.getClass();
        T bundle = mapper.convertValue(resource, clazz);
        bundle.setPayload(resource.getPayload());
        Map<String, Object> vars = mapper.convertValue(bundle, new TypeReference<Map<String, Object>>() {});

        // Adds the payload (because Jackson is set to ignore it)
        vars.put("payload", bundle.getPayload());

        return vars;
    }
}
