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

package gr.uoa.di.madgik.resourcecatalogue.service.sync;

import gr.uoa.di.madgik.resourcecatalogue.domain.Datasource;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiable;
import gr.uoa.di.madgik.resourcecatalogue.domain.Provider;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResource;
import gr.uoa.di.madgik.resourcecatalogue.service.SynchronizerService;
import jakarta.annotation.PostConstruct;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public abstract class AbstractSyncService<T extends Identifiable> implements SynchronizerService<T> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractSyncService.class);
    private static boolean isInitialized = false;

    protected WebClient webClient;
    protected boolean active = false;
    protected String host;
    protected String controller;
    private final String filename;

    private final BlockingQueue<Pair<T, String>> queue;

    protected abstract String getController();

    public AbstractSyncService(@Value("${sync.host:}") String host,
                               @Value("${sync.token.filepath:}") String filename,
                               @Value("${sync.enable}") boolean enabled,
                               WebClient.Builder webClientBuilder) {
        this.host = host;
        this.filename = filename;
        this.webClient = webClientBuilder.build();

        if (!host.isBlank() && enabled) {
            this.active = true;
        }

        this.queue = new LinkedBlockingQueue<>();
    }

    @PostConstruct
    void init() {
        this.controller = getController();
        if (!isInitialized) {
            if (filename.isBlank()) {
                logger.warn("'sync.token.filepath' value not set");
            }
            isInitialized = true;
        }
    }

    public BlockingQueue<Pair<T, String>> getQueue() {
        return queue;
    }

    @Scheduled(initialDelay = 0, fixedRate = 300000)
    public void retrySync() {
        int syncTries = 0;

        if (!queue.isEmpty()) {
            logger.warn("There are {} resources waiting to be Synchronized!", queue.size());
        }

        try {
            while (!queue.isEmpty() && syncTries < 1) {
                Pair<T, String> pair = queue.take();
                logger.info("Attempting to perform '{}' operation for the {}:\n{}", pair.getValue1(), pair.getValue0().getClass(), pair.getValue0());
                switch (pair.getValue1()) {
                    case "add" -> syncAdd(pair.getValue0());
                    case "update" -> syncUpdate(pair.getValue0());
                    case "delete" -> syncDelete(pair.getValue0());
                    default -> logger.warn("Unsupported action: {}", pair.getValue1());
                }
                syncTries++;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void syncAdd(T t) {
        if (!active) return;
        boolean retryKey = true;

        try {
            ResponseEntity<?> response = sendRequest(HttpMethod.POST, host + controller, t, t.getClass());
            if (response.getStatusCode() == HttpStatus.CREATED) retryKey = false;
            else logError("Adding", t, response);
        } catch (Exception e) {
            logException("syncAdd", t, e);
        }

        retryIfNecessary(t, "add", retryKey);
    }

    @Override
    public void syncUpdate(T t) {
        if (!active) return;
        boolean retryKey = true;

        try {
            ResponseEntity<?> response = sendRequest(HttpMethod.PUT, host + controller, t, t.getClass());
            if (response.getStatusCode() == HttpStatus.OK) retryKey = false;
            else logError("Updating", t, response);
        } catch (Exception e) {
            logException("syncUpdate", t, e);
        }

        retryIfNecessary(t, "update", retryKey);
    }

    @Override
    public void syncDelete(T t) {
        if (!active) return;
        boolean retryKey = true;

        String deleteUrl = String.format("%s/%s/%s", host, controller, t.getId());
        try {
            ResponseEntity<Void> response = sendRequestWithoutBody(HttpMethod.DELETE, deleteUrl, Void.class);
            if (response.getStatusCode() == HttpStatus.NO_CONTENT) retryKey = false;
            else logError("Deleting", t, response);
        } catch (Exception e) {
            logException("syncDelete", t, e);
        }

        retryIfNecessary(t, "delete", retryKey);
    }

    @Override
    public void syncVerify(T t) {
        if (!active) return;
        boolean retryKey = true;
        String uri;

        try {
            // Determine the correct verification endpoint
            uri = switch (t) {
                case Provider provider ->
                        host + controller + "/verifyProvider/" + t.getId() + "?active=true&status=approved%20provider";
                case TrainingResource trainingResource ->
                        host + controller + "/verifyTrainingResource/" + t.getId() + "?active=true&status=approved%20resource";
                case Datasource datasource ->
                        host + controller + "/verifyDatasource/" + t.getId() + "?active=true&status=approved%20resource";
                default ->
                        host + controller + "/verifyResource/" + t.getId() + "?active=true&status=approved%20resource";
            };

            ResponseEntity<?> response = sendRequest(HttpMethod.PATCH, uri, t, t.getClass());

            if (response != null && response.getStatusCode() == HttpStatus.OK) {
                retryKey = false;
            } else {
                logError("Verifying", t, response);
            }

        } catch (Exception e) {
            logException("syncVerify", t, e);
        }

        retryIfNecessary(t, "verify", retryKey);
    }

    private <R> ResponseEntity<R> sendRequest(HttpMethod method, String url, Object body, Class<R> responseType) {
        return webClient
                .method(method)
                .uri(url)
                .headers(headers -> headers.addAll(createHeaders()))
                .bodyValue(body)
                .retrieve()
                .toEntity(responseType)
                .block();
    }

    private <R> ResponseEntity<R> sendRequestWithoutBody(HttpMethod method, String url, Class<R> responseType) {
        return webClient
                .method(method)
                .uri(url)
                .headers(headers -> headers.addAll(createHeaders()))
                .retrieve()
                .toEntity(responseType)
                .block();
    }

    protected HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            String token = readFile(filename);
            headers.setBearerAuth(token);
        } catch (IOException e) {
            logger.error("Could not read file '{}' containing the synchronization token", filename, e);
        }
        return headers;
    }

    protected String readFile(String filename) throws IOException {
        return Files.readString(Path.of(filename)).trim();
    }

    private void logError(String operation, T t, ResponseEntity<?> response) {
        logger.error("{} {} with id '{}' from host [{}] returned code '{}'\nResponse body:\n{}",
                operation, t.getClass().getSimpleName(), t.getId(), host,
                response.getStatusCodeValue(), response.getBody());
    }

    private void logException(String operation, T t, Exception e) {
        if (e instanceof WebClientResponseException wcre) {
            logger.error("Failed to {} {} with id '{}' to host {}\nMessage: {}",
                    operation, t.getClass().getSimpleName(), t.getId(), host, wcre.getResponseBodyAsString());
        } else {
            logger.error("{} failed, check if token has expired!\n{}: {}", operation, t.getClass(), t, e);
        }
    }

    private void retryIfNecessary(T t, String operation, boolean retryKey) {
        if (retryKey) {
            try {
                queue.add(Pair.with(t, operation));
            } catch (IllegalStateException e) {
                logger.info("No space is currently available in the Queue");
            }
        }
    }
}
