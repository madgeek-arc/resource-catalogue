package gr.uoa.di.madgik.resourcecatalogue.onboarding.flowable.delegates;

import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.flowable.WorkflowVariableMapper;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class GetOrganisationDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(GetOrganisationDelegate.class);

    private final GenericResourceService genericResourceService;
    private final WorkflowVariableMapper workflowVariableMapper;

    public GetOrganisationDelegate(GenericResourceService genericResourceService, WorkflowVariableMapper workflowVariableMapper) {
        this.genericResourceService = genericResourceService;
        this.workflowVariableMapper = workflowVariableMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(DelegateExecution execution) {
        Map<String, Object> resource = (Map<String, Object>) execution.getVariable("resource");
        Map<String, Object> payload = resource != null ? (Map<String, Object>) resource.get("payload") : null;
        String orgId = payload != null ? (String) payload.get("resourceOwner") : null;
        logger.info("Running task 'get-organisation' | id: {}", orgId);
        if (orgId == null || orgId.isBlank()) {
            throw new IllegalStateException("resourceOwner is null or empty in resource payload");
        }
        Bundle org = genericResourceService.get("organisation", orgId);
        Map<String, Object> vars = new HashMap<>();
        workflowVariableMapper.putResourceBundle(vars, org, "organisation");
        execution.setVariable("organisation", vars.get("organisation"));
        execution.setVariable("organisation_class", vars.get("organisation_class"));
    }
}
