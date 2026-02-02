package gr.uoa.di.madgik.resourcecatalogue.controllers.registry.sqaaas;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class SqaaasAssessmentService {

    private final AssessmentRunner runner;

    public SqaaasAssessmentService(AssessmentRunner runner) {
        this.runner = runner;
    }

    @Async
    public CompletableFuture<String> startAssessment(String repoUrl, String branch) {
        String pipelineId = runner.createPipelineAndRun(repoUrl, branch);
        return CompletableFuture.completedFuture(pipelineId);
    }

    public JsonNode getStatus(String pipelineId) {
        return runner.getStatus(pipelineId);
    }

    public JsonNode getOutput(String pipelineId) {
        return runner.getOutput(pipelineId);
    }
}
