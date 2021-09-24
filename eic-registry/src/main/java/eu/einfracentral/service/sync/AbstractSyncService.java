package eu.einfracentral.service.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import eu.einfracentral.domain.Identifiable;
import eu.einfracentral.service.SynchronizerService;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public abstract class AbstractSyncService<T extends Identifiable> implements SynchronizerService<T> {
    private static final Logger logger = LogManager.getLogger(AbstractSyncService.class);

    private final int maxQuantity;
    private final String filename;

    protected final String host;
    protected final String self;

    protected Class<T> typeParameterClass;
    protected RestTemplate restTemplate;
    protected boolean active = false;
    protected String controller;

    private BlockingQueue<Pair<T, String>> queue;

    protected abstract String getController();

    public abstract boolean filterOut(T t, T previous);

    @Autowired
    public AbstractSyncService(@Value("${sync.host:}") String host,
                               @Value("${swagger.url:}") String self,
                               @Value("${sync.token.filepath:}") String filename,
                               @Value("${elastic.index.max_result_window:10000}") int maxQuantity,
                               Class<T> typeParameterClass) {
        this.typeParameterClass = typeParameterClass;
        this.maxQuantity = maxQuantity;
        this.host = host;
        this.self = self;
        this.filename = filename;
        restTemplate = new RestTemplate();
        if (!"".equals(host)) {
            active = true;
        }
        if ("".equals(filename)) {
            logger.warn("'sync.token.filepath' value not set");
        }
        this.queue = new LinkedBlockingQueue<>();
    }

    @PostConstruct
    void init() {
        this.controller = getController();
    }

    public BlockingQueue<Pair<T, String>> getQueue() {
        return queue;
    }

    @Scheduled(initialDelay = 0, fixedRate = 300000) //run every 5 min
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
                    case "add":
                        syncAdd(pair.getValue0());
                        break;
                    case "update":
                        syncUpdate(pair.getValue0());
                        break;
                    case "delete":
                        syncDelete(pair.getValue0());
                        break;
                    default:
                        logger.warn("Unsupported action: {}", pair.getValue1());
                }
                syncTries++;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void syncAdd(T t) {
        boolean retryKey = true;
        if (active) {
            HttpEntity<T> request = new HttpEntity<>(t, createHeaders());
            logger.info("Posting resource with id: {} - Host: {}", t.getId(), host);
            try {
                URI uri = new URI(host + controller).normalize();
                ResponseEntity<?> re = restTemplate.exchange(uri.normalize(), HttpMethod.POST, request, t.getClass());
                if (re.getStatusCode() == HttpStatus.CONFLICT) {
                    logger.warn("Resource already exists.. Performing update operation instead");
                    syncUpdate(t);
                    return;
                } else if (re.getStatusCode() != HttpStatus.CREATED) {
                    logger.error("Adding {} with id '{}' from host '{}' returned code '{}'\nResponse body:\n{}",
                            t.getClass(), t.getId(), host, re.getStatusCodeValue(), re.getBody());
                } else {
                    retryKey = false;
                }
            } catch (URISyntaxException e) {
                logger.error("could not create URI for host: {}", host, e);
            } catch (HttpServerErrorException e) {
                logger.error("Failed to post {} with id {} to host {}\nMessage: {}",
                        t.getClass(), t.getId(), host, e.getResponseBodyAsString());
            } catch (RuntimeException re) {
                logger.error("syncAdd failed, check if token has expired!\n{}: {}", t.getClass(), t, re);
            }
            if (retryKey) {
                try {
                    queue.add(Pair.with(t, "add"));
                } catch (IllegalStateException e) {
                    logger.info("No space is currently available in the Queue");
                }
            }
        }
    }

    @Override
    public void syncUpdate(T t, T previous) {
        if (!filterOut(t, previous)) {
            syncUpdate(t);
        }
    }

    protected void syncUpdate(T t) {
        boolean retryKey = true;
        if (active) {
            HttpEntity<T> request = new HttpEntity<>(t, createHeaders());
            logger.info("Updating {} with id: {} - Host: {}", t.getClass(), t.getId(), host);
            try {
                URI uri = new URI(host + controller).normalize();
                ResponseEntity<?> re = restTemplate.exchange(uri.normalize().toString(), HttpMethod.PUT, request, t.getClass());
                if (re.getStatusCode() != HttpStatus.OK) {
                    logger.error("Updating {} with id '{}' from host '{}' returned code '{}'\nResponse body:\n{}",
                            t.getClass(), t.getId(), host, re.getStatusCodeValue(), re.getBody());
                } else {
                    retryKey = false;
                }
            } catch (URISyntaxException e) {
                logger.error("could not create URI for host: {}", host, e);
            } catch (HttpServerErrorException e) {
                logger.error("Failed to update {} with id {} to host {}\nMessage: {}",
                        t.getClass(), t.getId(), host, e.getResponseBodyAsString());
            } catch (RuntimeException re) {
                logger.error("syncUpdate failed, check if token has expired!\n{}: {}", t.getClass(), t, re);
            }
            if (retryKey) {
                try {
                    queue.add(Pair.with(t, "update"));
                } catch (IllegalStateException e) {
                    logger.info("No space is currently available in the Queue");
                }
            }
        }
    }

    @Override
    public void syncDelete(T t) {
        boolean retryKey = true;
        if (active) {
            HttpEntity<T> request = new HttpEntity<>(createHeaders());
            logger.info("Deleting {} with id: {} - Host: {}", t.getClass(), t.getId(), host);
            try {
                URI uri = new URI(String.format("%s/%s/%s", host, controller, t.getId())).normalize();
                ResponseEntity<?> re = restTemplate.exchange(uri.toString(), HttpMethod.DELETE, request, Void.class);
                if (re.getStatusCode() != HttpStatus.NO_CONTENT) {
                    logger.error("Deleting {} with id '{}' from host '{}' returned code '{}'\nResponse body:\n{}",
                            t.getClass(), t.getId(), host, re.getStatusCodeValue(), re.getBody());
                } else {
                    retryKey = false;
                }
            } catch (URISyntaxException e) {
                logger.error("could not create URI for host: {}", host, e);
            } catch (HttpServerErrorException e) {
                logger.error("Failed to delete {} with id {} to host {}\nMessage: {}",
                        t.getClass(), t.getId(), host, e.getResponseBodyAsString());
            } catch (RuntimeException re) {
                logger.error("syncDelete failed, check if token has expired!\n{}: {}", t.getClass(), t, re);
            }
            if (retryKey) {
                try {
                    queue.add(Pair.with(t, "delete"));
                } catch (IllegalStateException e) {
                    logger.info("No space is currently available in the Queue");
                }
            }
        }
    }

    @Override
    public void syncAll() {
        logger.info("Retrieving resources from remote host: {}", host);
        Paging<T> remoteResources = getResources(host, maxQuantity);
        Paging<T> localResources = getResources(self, maxQuantity);
        if (remoteResources != null && localResources != null) {
            List<T> missingResources = localResources.getResults();
            List<T> updatedResources = new ArrayList<>();
            List<T> deletedResources = remoteResources.getResults();

            Iterator<T> remoteResourceIter = deletedResources.iterator();
            while (remoteResourceIter.hasNext()) {
                T remoteResource = remoteResourceIter.next();

                Iterator<T> localResourceIter = missingResources.iterator();
                while (localResourceIter.hasNext()) {
                    T localResource = localResourceIter.next();

                    if (localResource.getId().equals(remoteResource.getId())) {
                        if (!filterOut(localResource, remoteResource)) {
                            updatedResources.add(localResource);
                        }
                        localResourceIter.remove();
                        remoteResourceIter.remove();
                        break;
                    }
                }
            }
            logger.info("Missing Resources: {}", missingResources.size());
            logger.info("Updated Resources: {}", updatedResources.size());
            logger.info("Deleted Resources: {}", deletedResources.size());

            missingResources.forEach(this::syncAdd);
            updatedResources.forEach(this::syncUpdate);
            deletedResources.forEach(this::syncDelete);
        }
    }

    private Paging<T> getResources(String host, int quantity) {
        ResponseEntity<Paging> re = null;
        Paging<T> paging = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            HttpEntity<Void> request = new HttpEntity<>(createHeaders());
            // TODO: remove /all if it is removed from controllers
            URI remoteUri = new URI(host + controller + "/all?quantity=" + quantity).normalize();
            re = restTemplate.exchange(remoteUri.normalize(), HttpMethod.GET, request, Paging.class);
            if (re.getStatusCode() != HttpStatus.OK || !re.hasBody()) {
                logger.error("Retrieving '{}' resources from host '{}' returned code '{}'\nResponse body:\n{}",
                        typeParameterClass, host, re.getStatusCodeValue(), re.getBody());
                throw new ServiceException("Could not retrieve resources to sync catalogues");
            }
            paging = new Paging<>();
            paging.setFrom(re.getBody().getFrom());
            paging.setTo(re.getBody().getTo());
            paging.setTotal(re.getBody().getTotal());
            paging.setFacets(re.getBody().getFacets());

            List<T> results = new ArrayList<>();
            for (Object result : re.getBody().getResults()) {
                results.add(objectMapper.readValue(new Gson().toJson(result), typeParameterClass));
            }
            paging.setResults(results);

        } catch (URISyntaxException e) {
            logger.error("could not create URI for host: {}", host, e);
        } catch (NumberFormatException e) {
            logger.error("quantity given is not an Integer: {}", quantity, e);
        } catch (JsonProcessingException e) {
            logger.error(e);
        } catch (RuntimeException e) {
            logger.error("syncAll failed, check if token has expired!\n{}", re);
        }
        return paging;
    }

    protected HttpHeaders createHeaders() {
        String token;
        HttpHeaders headers = new HttpHeaders();
        try {
            token = readFile(filename);
            headers.add("Authorization", "Bearer " + token);
        } catch (IOException e) {
            logger.error("Could not read file '{}' containing the synchronization token", filename, e);
        }

        return headers;
    }

    protected String readFile(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
            return sb.toString();
        }
    }
}
