package gr.uoa.di.madgik.resourcecatalogue.controllers.registry.sqaaas;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Component
public class AssessmentRunner {

    private final SqaaasClient client;

    public AssessmentRunner(SqaaasClient client) {
        this.client = client;
    }

    // Start pipeline, return pipelineId immediately
    public String createPipelineAndRun(String repoUrl, String branch) {
        Map<String, Object> payload = Map.of(
                "repo_code", Map.of(
                        "repo", repoUrl,
                        "branch", branch
                )
        );

        String pipelineId = client.createPipeline(payload);

        try {
            client.runPipeline(pipelineId);
        } catch (WebClientResponseException e) {
            JsonNode output = client.getOutput(pipelineId);
            System.err.println("Pipeline run failed. Logs:");
            System.err.println(output.toPrettyString());
            throw e;
        }

        return pipelineId;
    }

    public JsonNode getStatus(String pipelineId) {
        return client.getStatus(pipelineId);
    }

    public JsonNode getOutput(String pipelineId) {
        return client.getOutput(pipelineId);
    }
}
