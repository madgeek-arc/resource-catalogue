package gr.uoa.di.madgik.resourcecatalogue.onboarding.flowable.delegates;

import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.flowable.ResourceBundleHelper;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateResourceDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(UpdateResourceDelegate.class);

    private final ResourceBundleHelper resourceBundleHelper;
    private final GenericResourceService genericResourceService;

    // Injected via <flowable:field> in BPMN
    private Expression resourceName;
    private Expression resourceType;

    public UpdateResourceDelegate(ResourceBundleHelper resourceBundleHelper, GenericResourceService genericResourceService) {
        this.resourceBundleHelper = resourceBundleHelper;
        this.genericResourceService = genericResourceService;
    }

    public void setResourceName(Expression resourceName) { this.resourceName = resourceName; }
    public void setResourceType(Expression resourceType) { this.resourceType = resourceType; }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(DelegateExecution execution) {
        String rName = resourceName != null ? (String) resourceName.getValue(execution) : "resource";
        String rType = resourceType != null ? (String) resourceType.getValue(execution) : null;
        if (rType == null || rType.isBlank()) {
            rType = (String) execution.getVariable("resourceType");
        }
        if (rType == null || rType.isBlank()) {
            throw new IllegalStateException("resourceType is required for update-resource task");
        }

        Map<String, Object> vars = new HashMap<>(execution.getVariables());

        Bundle resource = resourceBundleHelper.getResourceBundle(vars, rName);
        logger.info("Running task 'update-resource' | resourceType: {}, resourceName: {}, id: {}",
                rType, rName, resource.getId());

        // Validation is disabled by design // TODO: probably enable again now that code is synchronous?
        Bundle updated;
        try {
            updated = genericResourceService.update(rType, resource.getId(), resource, false);
        } catch (NoSuchFieldException | java.lang.reflect.InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Failed to update resource of type '" + rType + "' with id '" + resource.getId() + "'", e);
        }
        resourceBundleHelper.putResourceBundle(vars, updated, rName);

        execution.setVariable(rName, vars.get(rName));
        execution.setVariable(rName + "_class", vars.get(rName + "_class"));
    }
}
