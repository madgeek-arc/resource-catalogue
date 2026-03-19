package gr.uoa.di.madgik.resourcecatalogue.onboarding.flowable.delegates;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
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
public class OnboardResourceDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(OnboardResourceDelegate.class);

    private final ResourceBundleHelper resourceBundleHelper;

    // Injected via <flowable:field> in BPMN
    private Expression resourceName;
    private Expression status;
    private Expression active;
    private Expression comment;

    public OnboardResourceDelegate(ResourceBundleHelper resourceBundleHelper) {
        this.resourceBundleHelper = resourceBundleHelper;
    }

    public void setResourceName(Expression resourceName) { this.resourceName = resourceName; }
    public void setStatus(Expression status) { this.status = status; }
    public void setActive(Expression active) { this.active = active; }
    public void setComment(Expression comment) { this.comment = comment; }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(DelegateExecution execution) {
        String rName = resourceName != null ? (String) resourceName.getValue(execution) : "resource";
        String statusVal = status != null ? (String) status.getValue(execution) : "pending";
        boolean activeVal = active != null && "true".equalsIgnoreCase((String) active.getValue(execution));
        String commentVal = comment != null ? (String) comment.getValue(execution) : "";

        logger.info("Running task 'resource-onboard' | resourceName: {}, status: {}, active: {}",
                rName, statusVal, activeVal);

        Map<String, Object> vars = new HashMap<>();
        execution.getVariables().forEach(vars::put);

        Bundle bundle = resourceBundleHelper.getResourceBundle(vars, rName);
        UserInfo user = resourceBundleHelper.getUserInfo(vars);
        bundle.markOnboard(statusVal, activeVal, user, commentVal);
        resourceBundleHelper.putResourceBundle(vars, bundle, rName);

        execution.setVariable(rName, vars.get(rName));
        execution.setVariable(rName + "_class", vars.get(rName + "_class"));
    }
}
