package gr.uoa.di.madgik.resourcecatalogue.service.sync;

import gr.uoa.di.madgik.resourcecatalogue.domain.Datasource;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiable;
import gr.uoa.di.madgik.resourcecatalogue.domain.Provider;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResource;
import gr.uoa.di.madgik.resourcecatalogue.service.SynchronizerService;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public abstract class AbstractSyncService<T extends Identifiable> implements SynchronizerService<T> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractSyncService.class);
    private static boolean isInitialized = false;

    protected RestTemplate restTemplate;
    protected boolean active = false;
    protected String host;
    protected String controller;
    private String filename;

    private BlockingQueue<Pair<T, String>> queue;

    protected abstract String getController();

    @Autowired
    public AbstractSyncService(@Value("${sync.host:}") String host, @Value("${sync.token.filepath:}") String filename, @Value("${sync.enable}") boolean enabled) {
        this.host = host;
        this.filename = filename;
        restTemplate = new RestTemplate();

        if (!"".equals(host) && enabled) {
            active = true;
        }
        this.queue = new LinkedBlockingQueue<>();
    }

    @PostConstruct
    void init() {
        this.controller = getController();
        if (!isInitialized) {
            if ("".equals(filename)) {
                logger.warn("'sync.token.filepath' value not set");
            }
            isInitialized = true;
        }
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
                    case "verify":
                        syncVerify(pair.getValue0());
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
                if (re.getStatusCode() != HttpStatus.CREATED) {
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
    public void syncUpdate(T t) {
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

    //TODO: syncVerify is never used - maybe delete it
    @Override
    public void syncVerify(T t) {
        boolean retryKey = true;
        if (active) {
            HttpEntity<T> request = new HttpEntity<>(t, createHeaders());
            URI uri;
            logger.info("Verifying resource with id: {} - Host: {}", t.getId(), host);
            try {
                if (t instanceof Provider) {
                    uri = new URI(host + controller + "/verifyProvider/" + t.getId() + "?active=true&status=approved%20provider").normalize();
                } else if (t instanceof TrainingResource) {
                    uri = new URI(host + controller + "/verifyTrainingResource/" + t.getId() + "?active=true&status=approved%20resource").normalize();
                } else if (t instanceof Datasource) {
                    uri = new URI(host + controller + "/verifyDatasource/" + t.getId() + "?active=true&status=approved%20resource").normalize();
                } else {
                    uri = new URI(host + controller + "/verifyResource/" + t.getId() + "?active=true&status=approved%20resource").normalize();
                }
                HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
                restTemplate.setRequestFactory(requestFactory);
                ResponseEntity<?> re = restTemplate.exchange(uri.normalize(), HttpMethod.PATCH, request, t.getClass());
                if (re.getStatusCode() != HttpStatus.OK) {
                    logger.error("Verifying {} with id '{}' from host '{}' returned code '{}'\nResponse body:\n{}",
                            t.getClass(), t.getId(), host, re.getStatusCodeValue(), re.getBody());
                } else {
                    retryKey = false;
                }
            } catch (URISyntaxException e) {
                logger.error("could not create URI for host: {}", host, e);
            } catch (HttpServerErrorException e) {
                logger.error("Failed to patch {} with id {} to host {}\nMessage: {}",
                        t.getClass(), t.getId(), host, e.getResponseBodyAsString());
            } catch (RuntimeException re) {
                logger.error("syncVerify failed, check if token has expired!\n{}: {}", t.getClass(), t, re);
            }
            if (retryKey) {
                try {
                    queue.add(Pair.with(t, "verify"));
                } catch (IllegalStateException e) {
                    logger.info("No space is currently available in the Queue");
                }
            }
        }
    }

    protected HttpHeaders createHeaders() {
        String token;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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
