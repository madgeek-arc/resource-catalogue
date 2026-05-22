package gr.uoa.di.madgik.resourcecatalogue.onboarding.flowable;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WorkflowVariableMapper {

    private final ObjectMapper mapper;

    public WorkflowVariableMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public <T extends Bundle> T getResourceBundle(Map<String, Object> vars) {
        return getResourceBundle(vars, "resource");
    }

    public <T extends Bundle> void putResourceBundle(Map<String, Object> vars, T bundle) {
        putResourceBundle(vars, bundle, "resource");
    }

    @SuppressWarnings("unchecked")
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
        bundle.setPayload(mapper.convertValue(resource.get("payload"), new TypeReference<>() {}));
        return bundle;
    }

    @SuppressWarnings("unchecked")
    public <T extends Bundle> void putResourceBundle(Map<String, Object> vars, T bundle, String resourceName) {
        vars.put(resourceName + "_class", bundle.getClass().getName());
        vars.put(resourceName, toMap(bundle));
    }

    public UserInfo getUserInfo(Map<String, Object> vars) {
        return mapper.convertValue(vars.get("user"), UserInfo.class);
    }

    public void putUserInfo(Map<String, Object> vars, UserInfo user) {
        vars.put("user", mapper.convertValue(user, new TypeReference<Map<String, Object>>() {}));
    }

    @SuppressWarnings("unchecked")
    public <T extends Bundle> Map<String, Object> toMap(T resource) {
        Class<T> clazz = (Class<T>) resource.getClass();
        T bundle = mapper.convertValue(resource, clazz);
        bundle.setPayload(resource.getPayload());
        Map<String, Object> res = mapper.convertValue(bundle, new TypeReference<Map<String, Object>>() {});
        res.put("payload", bundle.getPayload());
        return res;
    }
}
