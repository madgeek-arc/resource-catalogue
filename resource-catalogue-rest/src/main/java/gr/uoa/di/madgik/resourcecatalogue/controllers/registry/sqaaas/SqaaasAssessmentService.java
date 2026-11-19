package gr.uoa.di.madgik.resourcecatalogue.controllers.registry.sqaaas;

import tools.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
public class SqaaasAssessmentService {

    private static final Logger logger = LoggerFactory.getLogger(SqaaasAssessmentService.class);
    private static final Duration TIMEOUT = Duration.ofMinutes(30);
    private static final long POLL_INTERVAL_MS = 30_000;
    // build_status enum of GET /pipeline/{id}/status in the SQAaaS API (v3.5.0); "success" is handled separately
    private static final Set<String> FAILED_STATUSES =
            Set.of("failure", "aborted", "not_built", "unstable", "waiting_scan_org");

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

    /**
     * Polls the pipeline until it ends. Returns the output on {@code success}; throws on any other final status
     * and on timeout, so the caller can report it instead of polling forever.
     */
    public JsonNode waitForCompletion(String pipelineId) {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            JsonNode node = runner.getStatus(pipelineId).get("build_status");
            String status = node == null || node.isNull() ? "" : node.asText().toLowerCase(Locale.ROOT);

            if ("success".equals(status)) {
                logger.info("SQA pipeline {} finished with status 'success'", pipelineId);
                return runner.getOutput(pipelineId);
            }
            if (FAILED_STATUSES.contains(status)) {
                throw new IllegalStateException(
                        String.format("SQA pipeline %s finished with status '%s'", pipelineId, status));
            }
            logger.debug("SQA pipeline {} not finished yet (status: '{}')", pipelineId, status);

            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for SQA pipeline " + pipelineId, e);
            }
        }
        throw new IllegalStateException(String.format("SQA pipeline %s did not finish within %d minutes",
                pipelineId, TIMEOUT.toMinutes()));
    }
}
