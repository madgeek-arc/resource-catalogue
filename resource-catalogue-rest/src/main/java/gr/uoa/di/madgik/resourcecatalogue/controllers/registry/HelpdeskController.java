package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Profile("beyond")
@RestController
@RequestMapping({"helpdesk"})
@Tag(name = "helpdesk")
public class HelpdeskController {

    @Value("${helpdesk.endpoint}")
    private String helpdeskEndpoint;

    private final WebClient webClient;

    public HelpdeskController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Operation(summary = "Returns a specific ticket.")
    @GetMapping(path = "tickets/{ticketId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getTicket(@PathVariable("ticketId") String ticketId) {
        try {
            Object ticket = webClient.get()
                    .uri(helpdeskEndpoint + "/tickets/{ticketId}", ticketId)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            return ResponseEntity.ok(ticket);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Operation(summary = "Submit a ticket.")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Object> submitTicket(@RequestBody Map<String, Object> ticketData,
                                               @Parameter(hidden = true) OAuth2AuthenticationToken token) {
        ticketData.put("userToken", ((DefaultOidcUser) token.getPrincipal()).getIdToken().getTokenValue());

        try {
            Object response = webClient.post()
                    .uri(helpdeskEndpoint)
                    .bodyValue(ticketData)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            return ResponseEntity.ok(response);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Operation(summary = "Submit articles in a ticket.")
    @PostMapping(path = "tickets/{ticketId}/articles", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Object> submitArticles(@PathVariable String ticketId,
                                                 @RequestBody Map<String, Object> articleData,
                                                 @Parameter(hidden = true) OAuth2AuthenticationToken token) {
        articleData.put("userToken", ((DefaultOidcUser) token.getPrincipal()).getIdToken().getTokenValue());

        try {
            Object response = webClient.post()
                    .uri(helpdeskEndpoint + "/tickets/" + ticketId + "/articles")
                    .bodyValue(articleData)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            return ResponseEntity.ok(response);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}