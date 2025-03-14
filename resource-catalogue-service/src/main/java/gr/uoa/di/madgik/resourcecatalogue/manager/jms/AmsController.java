package gr.uoa.di.madgik.resourcecatalogue.manager.jms;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("ams")
@Tag(name = "ams")
public class AmsController {

    private final AmsClient amsClient;

    public AmsController(AmsClient amsClient) {
        this.amsClient = amsClient;
    }

    @PutMapping("/topic")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String createTopic(@RequestParam String topic) {
        return amsClient.createTopic(topic);
    }

    @GetMapping("/topic/{topic}")
    public String getTopic(@PathVariable String topic) {
        return amsClient.getTopic(topic);
    }

    @GetMapping("/topic/all")
    public String getTopics() {
        return amsClient.getTopics();
    }

    @PostMapping("/topic/publish")
    public void publish(@RequestParam String topic, @RequestBody Object message) {
        amsClient.publishTopic(topic, message);
    }

    @GetMapping("/subscription/{topic}")
    public String getSubscriptions(@PathVariable String topic) {
        return amsClient.getSubscriptionsPerTopic(topic);
    }

    @GetMapping("/subscription/all")
    public String getSubscriptions() {
        return amsClient.getSubscriptions();
    }

    @PostMapping("/subscription/{topic}/create")
    public void createSubscription(@PathVariable String topic, @RequestParam String name) {
        amsClient.createSubscription(topic, name);
    }
}

