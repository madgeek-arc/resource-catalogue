package gr.uoa.di.madgik.resourcecatalogue.onboarding;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import org.springframework.security.core.Authentication;

public interface WorkflowService {

    public enum WorkflowStatus {
        SUCCESS,
        FAILURE
    }

    public record WorkflowResult(WorkflowStatus status, Integer code, String message) {
    }

    public <T extends Bundle> T onboard(String resourceType, T bundle, Authentication authentication);
}
