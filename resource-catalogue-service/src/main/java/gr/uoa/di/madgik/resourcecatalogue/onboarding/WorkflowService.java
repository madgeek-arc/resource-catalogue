package gr.uoa.di.madgik.resourcecatalogue.onboarding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.CatalogueBundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.service.CatalogueService;
import io.camunda.client.CamundaClient;
import io.camunda.client.annotation.CustomHeaders;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.VariablesAsType;
import io.camunda.client.api.response.ProcessInstanceResult;
import io.camunda.client.exception.BpmnError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@Component
public class WorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowService.class);

    private final GenericResourceService genericResourceService;
    private final ObjectMapper mapper;
    private final CamundaClient client;

    public WorkflowService(GenericResourceService genericResourceService,
                           ObjectMapper objectMapper,
                           CamundaClient client) {
        this.genericResourceService = genericResourceService;
        this.mapper = objectMapper;
        this.client = client;
    }

    public enum WorkflowStatus {
        SUCCESS,
        FAILURE
    }

    public record WorkflowResult(WorkflowStatus status, Integer code, String message) {
    }

    public <T extends Bundle> T onboard(String resourceType, T bundle, Authentication authentication) {
        String bpmnProcess = getBpmnProcess(resourceType);
        Map<String, Object> vars = new HashMap<>();
        vars.put("resourceType", resourceType);
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
        WorkflowResult result = getWorkflowResult(key);
        if (result.status() == WorkflowStatus.FAILURE) {
            throw new ResourceException(result.message(), HttpStatus.valueOf(result.code()));
        }
        bundle = getResourceBundle(key.getVariablesAsMap());
        logger.info("Onboarding for resource with id '{}' successful. Message: {}", bundle.getId(), result.message());
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
    public <T extends Bundle> Map<String, Object> onboardResource(@VariablesAsType Map<String, Object> vars,
                                               @CustomHeaders Map<String, String> headers) {
        String resourceName = headers.getOrDefault("resourceName", "resource");
        String status = headers.get("status");
        String active = headers.getOrDefault("active", "false");
        String comment = headers.getOrDefault("comment", "");

        T bundle = getResourceBundle(vars);
        UserInfo user = getUserInfo(vars);
        bundle.markOnboard(status, "true".equalsIgnoreCase(active), user, comment);

        logger.info("Running task 'resource-onboard' | resourceType: {}, id: {}, status: {}, active: {}", resourceName, bundle.getId(), status, active);

        return Map.of(resourceName, toMap(bundle));
    }

    @JobWorker(type = "get-resource", autoComplete = true)
    public <T extends Bundle> Map<String, Object> getResourceBundle(@VariablesAsType Map<String, Object> vars,
                                                 @CustomHeaders Map<String, String> headers) {
        String id = (String) vars.get("id");
        String resourceType = getResourceType(vars, headers);
        String resourceName = headers.getOrDefault("resourceName", "resource");
        logger.info("Running task 'get-resource' | resourceType: {}, id: {}", resourceType, id);
        T bundle = genericResourceService.get(resourceType, id);
        putResourceBundle(vars, bundle, resourceName);
        return vars;
    }

    @JobWorker(type = "update-resource", autoComplete = true)
    public <T extends Bundle> Map<String, Object> updateResource(@VariablesAsType Map<String, Object> vars,
                                 @CustomHeaders Map<String, String> headers) throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException {
        String resourceName = headers.getOrDefault("resourceName", "resource");
        String resourceType = getResourceType(vars, headers);
        T resource = getResourceBundle(vars, resourceName);
        logger.info("Running task 'update-resource' | resourceType: {}, id: {}", resourceType, resource.getId());
        T bundle = genericResourceService.update(resourceType, resource.getId(), resource);
        putResourceBundle(vars, bundle, resourceName);
        return vars;
    }

    @JobWorker(type = "delete-resource", autoComplete = true)
    public <T extends Bundle> Map<String, Object> deleteResource(@VariablesAsType Map<String, Object> vars,
                                 @CustomHeaders Map<String, String> headers) {
        String id = (String) vars.get("id");
        String resourceType = getResourceType(vars, headers);
        logger.info("Running task 'delete-resource' | resourceType: {}, id: {}", resourceType, id);
        T bundle = genericResourceService.get(resourceType, id);
        if (!bundle.isActive() && bundle.getStatus() == null) {
            genericResourceService.delete(resourceType, id);
        }
        return vars;
    }

    @JobWorker(type = "get-catalogue", autoComplete = true)
    public Map<String, Object> getCatalogue(@VariablesAsType Map<String, Object> vars) {
        var catalogueId = (String) vars.get("catalogueId");
        CatalogueBundle catalogue = null;
        try {
            catalogue = genericResourceService.get("catalogue", catalogueId);
        } catch (Exception ignore) {}
        vars.put("catalogue", catalogue);
        return vars;
    }

    public <T extends Bundle> T getResourceBundle(Map<String, Object> vars) {
        return getResourceBundle(vars, "resource");
    }

    public <T extends Bundle> void putResourceBundle(Map<String, Object> vars, T bundle) {
        putResourceBundle(vars, bundle, "resource");
    }

    public <T extends Bundle> T getResourceBundle(Map<String, Object> vars, String resourceName) {
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

    public <T extends Bundle> void putResourceBundle(Map<String, Object> vars, T bundle, String resourceName) {
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

    private <T extends Bundle> Map<String, Object> toMap(T resource) {
        Class<T> clazz = (Class<T>) resource.getClass();
        T bundle = mapper.convertValue(resource, clazz);
        bundle.setPayload(resource.getPayload());
        Map<String, Object> res = mapper.convertValue(bundle, new TypeReference<Map<String, Object>>() {});

        // Adds the payload (because Jackson is set to ignore it)
        res.put("payload", bundle.getPayload());

        return res;
    }

    private String getResourceType(Map<String, Object> vars, Map<String, String> headers) {
        String resourceType;
        if (headers.containsKey("resourceType") && !((String) headers.get("resourceType")).isEmpty()) {
            resourceType = (String) headers.get("resourceType");
        } else {
            resourceType = (String) vars.get("resourceType");
        }
        if (resourceType == null || resourceType.isBlank()) {
            throw new BpmnError("RESOURCE_TYPE_MISSING", "resourceType is required");
        }
        return resourceType;
    }

    private WorkflowResult getWorkflowResult(ProcessInstanceResult key) {
        if (!key.getVariablesAsMap().containsKey("workflowResult")) {
            return new WorkflowResult(WorkflowStatus.SUCCESS, 200, "Success");
        }
        return mapper.convertValue(key.getVariablesAsMap().get("workflowResult"), WorkflowResult.class);
    }
}
