package gr.uoa.di.madgik.resourcecatalogue.onboarding.flowable.delegates;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Generic delegate that sets a key/value pair on a Map process variable.
 * Replaces FEEL script tasks like: context put(resource, "auditState", "Not audited")
 *
 * Fields injected via flowable:field:
 * - variableName: name of the process variable (Map) to modify (e.g., "resource", "organisation")
 * - key:          the map key to set
 * - value:        the string value to set
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SetKeyValueDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(SetKeyValueDelegate.class);

    private Expression variableName;
    private Expression key;
    private Expression value;

    public void setVariableName(Expression variableName) { this.variableName = variableName; }
    public void setKey(Expression key) { this.key = key; }
    public void setValue(Expression value) { this.value = value; }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(DelegateExecution execution) {
        String varName = variableName != null ? (String) variableName.getValue(execution) : "resource";
        String k = key != null ? (String) key.getValue(execution) : null;
        Object v = value != null ? value.getValue(execution) : null;

        if (k == null) {
            throw new IllegalStateException("key field is required for SetKeyValueDelegate");
        }

        Map<String, Object> map = (Map<String, Object>) execution.getVariable(varName);
        if (map == null) {
            throw new IllegalStateException("Process variable '" + varName + "' is null");
        }
        logger.debug("SetKeyValueDelegate | variable: {}, key: {}, value: {}", varName, k, v);
        map.put(k, v);
        execution.setVariable(varName, map);
    }
}
