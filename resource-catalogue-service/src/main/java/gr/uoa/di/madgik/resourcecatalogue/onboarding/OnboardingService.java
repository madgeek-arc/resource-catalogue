package gr.uoa.di.madgik.resourcecatalogue.onboarding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.CatalogueBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.CatalogueService;
import io.camunda.client.annotation.CustomHeaders;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.VariablesAsType;
import io.camunda.client.exception.BpmnError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

@Component
public class OnboardingService {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingService.class);

    private final CatalogueService catalogueService;
    private final GenericResourceService genericResourceService;
    private final ObjectMapper mapper;

    public OnboardingService(CatalogueService catalogueService,
                             GenericResourceService genericResourceService,
                             ObjectMapper objectMapper) {
        this.catalogueService = catalogueService;
        this.genericResourceService = genericResourceService;
        this.mapper = objectMapper;
    }

    @JobWorker(type = "provider-set-status", autoComplete = true)
    public Map<String, Object> setProviderStatus(@VariablesAsType Map<String, Object> vars,
                                                 @CustomHeaders Map<String, String> headers) {
        var providerId = (String) vars.get("providerId");
        var status = headers.getOrDefault("status", "PENDING");
        logger.info("Running task 'provider-set-status' for provider with id '{}' | status: {}", providerId, status);
        vars.put("status", status);
        return vars;
    }

    @JobWorker(type = "get-resource", autoComplete = true)
    public Map<String, Object> getResource(@VariablesAsType Map<String, Object> vars,
                                 @CustomHeaders Map<String, String> headers) {
        var id = (String) vars.get("id");
        var resourceType = headers.getOrDefault("resourceType", "resourceTypes");
        logger.info("Reading resource with resourceType '{}' and id '{}'", resourceType, id);
        Object bundle = genericResourceService.get(resourceType, id);
        return Map.of("resource", toMap((Bundle) bundle));
    }

    @JobWorker(type = "set-status", autoComplete = true)
    public Map<String, Object> setStatus(@VariablesAsType Map<String, Object> vars,
                               @CustomHeaders Map<String, String> headers)
            throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException {
        Bundle bundle = toBundle((Map<String, Object>) vars.get("resource"));
        var status = headers.getOrDefault("status", "PENDING");
        var active = Boolean.parseBoolean(headers.getOrDefault("active", "true"));
        var resourceType = "provider";
        logger.info("Running task 'set-status' for '{}' with id '{}' | status: {}", resourceType, bundle.getId(), status);
        bundle.markOnboard(status, active, SecurityContextHolder.getContext().getAuthentication(), null);
        var ret = genericResourceService.update(resourceType, bundle.getId(), bundle);
        vars.put("bundle", ret);
        return Map.of("bundle", ret);
    }

    @JobWorker(type = "error-handler", autoComplete = true)
    public Map<String, Object> handleError(@VariablesAsType Map<String, Object> vars,
                                           @CustomHeaders Map<String, String> headers) {
        var code = String.valueOf((int) vars.get("code"));
        var error = (String) vars.get("error");
        throw new BpmnError(code, error);
    }

    @JobWorker(type = "get-catalogue", autoComplete = true)
    public Map<String, Object> getCatalogue(@VariablesAsType Map<String, Object> vars) {
        var catalogueId = ((Map<String, String>) vars.get("payload")).get("catalogueId");
        CatalogueBundle catalogue = null;
        try {
            catalogue = catalogueService.get(catalogueId);
        } catch (Exception ignore) {}
        vars.put("catalogue", catalogue);
        return vars;
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
}
