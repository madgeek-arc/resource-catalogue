package gr.uoa.di.madgik.resourcecatalogue.onboarding.flowable.delegates;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Delegate that marks the workflow result (success or failure).
 * Replaces FEEL script tasks that create the workflowResult variable.
 *
 * Fields injected via flowable:field:
 * - status:  "SUCCESS" or "FAILURE"
 * - code:    HTTP status code (e.g., "200", "403")
 * - message: result message
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class MarkWorkflowResultDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(MarkWorkflowResultDelegate.class);

    private Expression status;
    private Expression code;
    private Expression message;

    public void setStatus(Expression status) { this.status = status; }
    public void setCode(Expression code) { this.code = code; }
    public void setMessage(Expression message) { this.message = message; }

    @Override
    public void execute(DelegateExecution execution) {
        String statusVal = status != null ? (String) status.getValue(execution) : "SUCCESS";
        Object codeRaw = code != null ? code.getValue(execution) : "200";
        String messageVal = message != null ? (String) message.getValue(execution) : "";

        int codeVal = codeRaw instanceof Integer i ? i : Integer.parseInt(codeRaw.toString());

        logger.debug("MarkWorkflowResultDelegate | status: {}, code: {}, message: {}", statusVal, codeVal, messageVal);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", statusVal);
        result.put("code", codeVal);
        result.put("message", messageVal);
        execution.setVariable("workflowResult", result);
    }
}
