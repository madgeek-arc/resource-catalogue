package gr.uoa.di.madgik.resourcecatalogue.controllers.registry.sqaaas;

import tools.jackson.databind.JsonNode;
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
        return CompletableFuture.completedFuture(runner.createPipelineAndRun(repoUrl, branch));
    }

    public JsonNode getStatus(String pipelineId) {
        return runner.getStatus(pipelineId);
    }

    public JsonNode getOutput(String pipelineId) {
        return runner.getOutput(pipelineId);
    }

    public JsonNode waitForCompletion(String pipelineId) {
        while (true) {
            JsonNode status = runner.getStatus(pipelineId);
            String buildStatus = status.get("build_status").asText();

            if ("SUCCESS".equalsIgnoreCase(buildStatus)) {
                return runner.getOutput(pipelineId);
            }

            try {
                Thread.sleep(30000); // check every 30s
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }
}
