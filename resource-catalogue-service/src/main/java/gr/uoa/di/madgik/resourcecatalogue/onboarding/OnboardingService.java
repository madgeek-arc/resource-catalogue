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
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.exception.BpmnError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
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

    public T onboard(T bundle) {
        Map<String, Object> provider = toMap(bundle);
        var key = client.newCreateInstanceCommand()
                .bpmnProcessId("provider-onboarding")
                .latestVersion()
                .variables(provider)
                .withResult()
                .requestTimeout(java.time.Duration.ofSeconds(120))
                .send()
                .join();
        bundle = toBundle((Map<String, Object>) key.getVariablesAsMap().get("resource"));
        bundle.markOnboard(bundle.getStatus(), bundle.isActive(), UserInfo.of(SecurityContextHolder.getContext().getAuthentication()), null);
        return toBundle(key.getVariablesAsMap());
    }

    @JobWorker(type = "resource-onboard", autoComplete = true)
    public Map<String, Object> onboardResource(final ActivatedJob job) {

        final var headers = job.getCustomHeaders();
        String resourceName = headers.getOrDefault("resourceName", "resource");
        String status = headers.get("status");
        String active = headers.get("active");

        if (status == null || status.isBlank()) {
            throw new BpmnError("MISSING_STATUS", "Status is required.");
        }

        final Map<String, Object> vars = job.getVariablesAsMap();
        T bundle = toBundle((Map<String, Object>) vars.get(resourceName));
        bundle.markOnboard(status, active.equalsIgnoreCase("true"), null, "change status job");

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

    @JobWorker(type = "resource-status.apply", autoComplete = true)
    public Map<String, Object> assignStatus(final ActivatedJob job) {

        final var headers = job.getCustomHeaders();
        String resourceName = headers.getOrDefault("resourceName", "resource");
        String status = headers.get("status");
        String active = headers.get("active");

        if (status == null || status.isBlank()) {
            throw new BpmnError("MISSING_STATUS", "Status is required.");
        }

        final Map<String, Object> vars = job.getVariablesAsMap();
        T bundle = toBundle((Map<String, Object>) vars.get(resourceName));
        bundle.markOnboard(status, active.equalsIgnoreCase("true"), null, "change status job");

        logger.info("Running task 'resource-status.apply' for {} with id '{}' | status: {}", resourceName, bundle.getId(), status);

        return Map.of(resourceName, toMap(bundle));
    }

    @JobWorker(type = "resource-status.persist", autoComplete = true)
    public Map<String, Object> persistStatus(@VariablesAsType Map<String, Object> vars,
                               @CustomHeaders Map<String, String> headers)
            throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException {
        final String resourceName = headers.getOrDefault("resourceName", "resource");
        T bundle = toBundle((Map<String, Object>) vars.get(resourceName));
        var status = headers.getOrDefault("status", "pending");
        var active = Boolean.parseBoolean(headers.getOrDefault("active", "true"));
        var resourceType = "provider";
        logger.info("Running task 'set-status' for '{}' with id '{}' | status: {}", resourceType, bundle.getId(), status);
        var ret = genericResourceService.update(resourceType, bundle.getId(), bundle);
//        vars.put(resourceVar, ret);
        return Map.of(resourceName, ret);
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

    public T toBundle(Map<String, Object> resource) {
        T bundle = mapper.convertValue(resource, new TypeReference<>() {});
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
