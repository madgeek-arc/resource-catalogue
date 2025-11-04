/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.resourcecatalogue.config.HelpdeskProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.util.Map;

@Profile("beyond")
@RestController
@RequestMapping({"helpdesk"})
@Tag(name = "helpdesk")
public class HelpdeskController {

    private static final Logger logger = LoggerFactory.getLogger(HelpdeskController.class);

    private final WebClient webClient;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public HelpdeskController(HelpdeskProperties helpdeskProperties,
                              OAuth2AuthorizedClientService authorizedClientService,
                              WebClient.Builder webClientBuilder) {
        this.authorizedClientService = authorizedClientService;
        if (helpdeskProperties.isEnabled()) {
            this.webClient = webClientBuilder
                    .baseUrl(helpdeskProperties.getEndpoint())
                    .build();
        } else {
            this.webClient = null;
        }
    }

    private String getAccessToken(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken token) {
            OAuth2AuthorizedClient authorizedClient =
                    authorizedClientService.loadAuthorizedClient(
                            token.getAuthorizedClientRegistrationId(),
                            token.getName());
            return authorizedClient.getAccessToken().getTokenValue();
        } else {
            throw new InsufficientAuthenticationException("Insufficient authentication");
        }
    }

    @Operation(summary = "Returns a specific ticket for the authenticated user.")
    @GetMapping(path = "tickets/{ticketId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Object> getTicket(@PathVariable("ticketId") String ticketId,
                                            @Parameter(hidden = true) Authentication authentication) {
        if (webClient == null) {
            throw new UnsupportedOperationException("Helpdesk service is not enabled.");
        }

        String accessToken = getAccessToken(authentication);

        try {
            Object ticket = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("ticket_id", ticketId)
                            .build())
                    .headers(headers -> headers.setBearerAuth(accessToken))
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


    @Operation(summary = "Returns all tickets for the authenticated user.")
    @GetMapping(path = "tickets", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Object> getAllTickets(@Parameter(hidden = true) Authentication authentication) {
        if (webClient == null) {
            throw new UnsupportedOperationException("Helpdesk service is not enabled.");
        }

        String accessToken = getAccessToken(authentication);

        try {
            Object tickets = webClient.get()
                    .uri(UriBuilder::build)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            return ResponseEntity.ok(tickets);
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
                                               @Parameter(hidden = true) Authentication authentication) {
        if (webClient == null) {
            throw new UnsupportedOperationException("Helpdesk service is not enabled.");
        }
        String accessToken = getAccessToken(authentication);

        try {
            Object response = webClient.post()
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .bodyValue(ticketData)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            logger.info("Ticket submitted successfully");
            return ResponseEntity.ok(response);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Operation(summary = "Update a ticket.")
    @PutMapping(path = "tickets/{ticketId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Object> updateTicket(@PathVariable("ticketId") String ticketId,
                                               @RequestBody Map<String, Object> ticketData,
                                               @Parameter(hidden = true) Authentication authentication) {
        if (webClient == null) {
            throw new UnsupportedOperationException("Helpdesk service is not enabled.");
        }
        String accessToken = getAccessToken(authentication);

        try {
            Object ticket = webClient.put()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("ticket_id", ticketId)
                            .build())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .bodyValue(ticketData)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            logger.info("Ticket with id [{}] updated successfully", ticketId);
            return ResponseEntity.ok(ticket);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}