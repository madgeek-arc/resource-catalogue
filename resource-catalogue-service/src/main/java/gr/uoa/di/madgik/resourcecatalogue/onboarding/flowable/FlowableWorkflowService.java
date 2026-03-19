package gr.uoa.di.madgik.resourcecatalogue.onboarding.flowable;

import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.WorkflowService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Primary
public class FlowableWorkflowService implements WorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(FlowableWorkflowService.class);

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final ResourceBundleHelper helper;

    public FlowableWorkflowService(RuntimeService runtimeService,
                                   HistoryService historyService,
                                   ResourceBundleHelper helper) {
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.helper = helper;
    }

    public enum WorkflowStatus {
        SUCCESS, FAILURE
    }

    public record WorkflowResult(WorkflowStatus status, Integer code, String message) {}

    public <T extends Bundle> T onboard(String resourceType, T bundle, Authentication authentication) {
        String bpmnProcess = getBpmnProcess(resourceType);
        Map<String, Object> vars = new HashMap<>();
        vars.put("resourceType", resourceType);
        helper.putResourceBundle(vars, bundle);
        helper.putUserInfo(vars, UserInfo.of(authentication));

        ProcessInstance process = runtimeService.startProcessInstanceByKey(bpmnProcess, vars);

        Map<String, Object> resultVars = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(process.getId())
                .list()
                .stream()
                .collect(Collectors.toMap(
                        HistoricVariableInstance::getVariableName,
                        HistoricVariableInstance::getValue
                ));

        WorkflowResult result = getWorkflowResult(resultVars);
        if (result.status() == WorkflowStatus.FAILURE) {
            throw new ResourceException(result.message(), HttpStatus.valueOf(result.code()));
        }
        bundle = helper.getResourceBundle(resultVars);
        logger.info("Onboarding for resource with id '{}' successful. Message: {}", bundle.getId(), result.message());
        return bundle;
    }

    private String getBpmnProcess(String resourceType) {
        return switch (resourceType) {
            case "organisation" -> "onboard-provider-flowable";
            case "service", "datasource", "training_resource", "deployable_application" -> "onboard-resource-flowable";
            case "adapter" -> "onboard-adapter-flowable";
            case "interoperability_record" -> "onboard-guideline-flowable";
            default -> throw new IllegalStateException("Unhandled onboarding for resourceType: " + resourceType);
        };
    }

    @SuppressWarnings("unchecked")
    private WorkflowResult getWorkflowResult(Map<String, Object> vars) {
        if (!vars.containsKey("workflowResult")) {
            return new WorkflowResult(WorkflowStatus.SUCCESS, 200, "Success");
        }
        Object raw = vars.get("workflowResult");
        if (raw instanceof Map<?, ?> resultMap) {
            String status = (String) resultMap.get("status");
            Object codeObj = resultMap.get("code");
            Integer code = codeObj instanceof Integer i ? i : Integer.valueOf(codeObj.toString());
            String message = (String) resultMap.get("message");
            return new WorkflowResult(WorkflowStatus.valueOf(status), code, message);
        }
        throw new IllegalStateException("Unexpected workflowResult type: " + raw.getClass());
    }
}
