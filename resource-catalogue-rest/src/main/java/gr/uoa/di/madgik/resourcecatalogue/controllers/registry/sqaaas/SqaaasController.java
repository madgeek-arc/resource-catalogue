package gr.uoa.di.madgik.resourcecatalogue.controllers.registry.sqaaas;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("sqaaas")
@Tag(name = "sqaaas")
public class SqaaasController {

    private final SqaaasAssessmentService service;

    public SqaaasController(SqaaasAssessmentService service) {
        this.service = service;
    }

    @PostMapping("/assess")
    public Map<String, String> assess(@RequestBody AssessmentRequest request) {
        CompletableFuture<String> future = service.startAssessment(
                request.getRepoUrl(),
                request.getBranch()
        );

        String pipelineId = future.join();
        return Map.of("pipelineId", pipelineId);
    }

    @GetMapping("/status/{pipelineId}")
    public JsonNode getStatus(@PathVariable String pipelineId) {
        return service.getStatus(pipelineId);
    }

    @GetMapping("/output/{pipelineId}")
    public JsonNode getOutput(@PathVariable String pipelineId) {
        return service.getOutput(pipelineId);
    }

    public static class AssessmentRequest {
        private String repoUrl;
        private String branch = "main";

        public String getRepoUrl() {
            return repoUrl;
        }

        public void setRepoUrl(String repoUrl) {
            this.repoUrl = repoUrl;
        }

        public String getBranch() {
            return branch;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }
    }
}
