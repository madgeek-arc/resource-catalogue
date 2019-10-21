package eu.einfracentral.service;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Measurement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class SynchronizerService {

    private static final Logger logger = LogManager.getLogger(SynchronizerService.class);

    private RestTemplate restTemplate;
    private boolean active = false;
    private String host;
    private String filename;
    private boolean retryKey;

    private BlockingQueue<InfraService> serviceQueue;
    private BlockingQueue<Measurement> measurementQueue;
    private BlockingQueue<String> serviceActionQueue;
    private BlockingQueue<String> measurementActionQueue;

    @Autowired
    public SynchronizerService(@Value("${sync.host:}") String host, @Value("${sync.token.filepath:}") String filename) {
        this.host = host;
        this.filename = filename;
        restTemplate = new RestTemplate();
        if (!"".equals(host)) {
            active = true;
        }
        if ("".equals(filename)) {
            logger.warn("'sync.token.filepath' value not set");
        }
        this.serviceQueue = new LinkedBlockingQueue<>();
        this.measurementQueue = new LinkedBlockingQueue<>();
        this.serviceActionQueue = new LinkedBlockingQueue<>();
        this.measurementActionQueue = new LinkedBlockingQueue<>();
    }

    public BlockingQueue<InfraService> getServiceQueue() {
        return serviceQueue;
    }

    public BlockingQueue<Measurement> getMeasurementQueue() {
        return measurementQueue;
    }

    public BlockingQueue<String> getServiceAction() {
        return serviceActionQueue;
    }

    public BlockingQueue<String> getMeasurementAction() {
        return measurementActionQueue;
    }

    private HttpHeaders createHeaders() {
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

    public void syncAdd(InfraService infraService) {
        retryKey = true;
        if (active) {
            HttpEntity<InfraService> request = new HttpEntity<>(infraService, createHeaders());
            logger.info("Posting service with id: {} - Host: {}", infraService.getId(), host);
            try {
                URI uri = new URI(host + "/infraService");
                ResponseEntity re = restTemplate.exchange(uri.normalize(), HttpMethod.POST, request, InfraService.class);
                if (re.getStatusCode() != HttpStatus.CREATED) {
                    logger.error("Adding service with id '{}' from host '{}' returned code '{}'\nResponse body:\n{}",
                            infraService.getId(), host, re.getStatusCodeValue(), re.getBody());
                } else {
                    retryKey = false;
                }
            } catch (URISyntaxException e) {
                logger.error("could not create URI for host: {}", host, e);
            } catch (HttpServerErrorException e) {
                logger.error("Failed to post Service with id {} to host {}\nMessage: {}",
                        infraService.getId(), host, e.getResponseBodyAsString());
            } catch (RuntimeException re) {
                logger.error("syncAdd failed, check if token has expired!\nService: {}", infraService.toString(), re);
            }
            if (retryKey) {
                try {
                    serviceQueue.add(infraService);
                    serviceActionQueue.add("add");
                } catch(IllegalStateException e){
                    logger.info("No space is currently available in the Queue");
                }
            }
        }
    }

    public void syncUpdate(InfraService infraService) {
        retryKey = true;
        if (active) {
            HttpEntity<InfraService> request = new HttpEntity<>(infraService, createHeaders());
            logger.info("Updating service with id: {} - Host: {}", infraService.getId(), host);
            try {
                URI uri = new URI(host + "/infraService");
                ResponseEntity re = restTemplate.exchange(uri.normalize().toString(), HttpMethod.PUT, request, InfraService.class);
                if (re.getStatusCode() != HttpStatus.OK) {
                    logger.error("Updating service with id '{}' from host '{}' returned code '{}'\nResponse body:\n{}",
                            infraService.getId(), host, re.getStatusCodeValue(), re.getBody());
                } else {
                    retryKey = false;
                }
            } catch (URISyntaxException e) {
                logger.error("could not create URI for host: " + host, e);
            } catch (HttpServerErrorException e) {
                logger.error("Failed to update Service with id {} to host {}\nMessage: {}",
                        infraService.getId(), host, e.getResponseBodyAsString());
            } catch (RuntimeException re) {
                logger.error("syncUpdate failed, check if token has expired!\nService: {}", infraService.toString(), re);
            }
            if (retryKey) {
                try {
                    serviceQueue.add(infraService);
                    serviceActionQueue.add("update");
                } catch(IllegalStateException e){
                    logger.info("No space is currently available in the Queue");
                }
            }
        }
    }

    public void syncDelete(InfraService infraService) {
        retryKey = true;
        if (active) {
            HttpEntity<InfraService> request = new HttpEntity<>(createHeaders());
            logger.info("Deleting service with id: {} - Host: {}", infraService.getId(), host);
            try {
                URI uri = new URI(String.format("%s/infraService/%s/%s", host, infraService.getId(), infraService.getVersion()));
                ResponseEntity re = restTemplate.exchange(uri.normalize().toString(), HttpMethod.DELETE, request, Void.class);
                if (re.getStatusCode() != HttpStatus.NO_CONTENT) {
                    logger.error("Deleting service with id '{}' from host '{}' returned code '{}'\nResponse body:\n{}",
                            infraService.getId(), host, re.getStatusCodeValue(), re.getBody());
                } else {
                    retryKey = false;
                }
            } catch (URISyntaxException e) {
                logger.error("could not create URI for host: {}", host, e);
            } catch (HttpServerErrorException e) {
                logger.error("Failed to delete Service with id {} to host {}\nMessage: {}",
                        infraService.getId(), host, e.getResponseBodyAsString());
            } catch (RuntimeException re) {
                logger.error("syncDelete failed, check if token has expired!\nService: {}", infraService.toString(), re);
            }
            if (retryKey) {
                try {
                    serviceQueue.add(infraService);
                    serviceActionQueue.add("delete");
                } catch(IllegalStateException e){
                    logger.info("No space is currently available in the Queue");
                }
            }
        }
    }

    public void syncAdd(Measurement measurement) {
        retryKey = true;
        if (active) {
            HttpEntity<Measurement> request = new HttpEntity<>(measurement, createHeaders());
            logger.info("Posting measurement with id: {} - Host: {}", measurement.getId(), host);
            try {
                URI uri = new URI(host + "/measurement");
                ResponseEntity re = restTemplate.exchange(uri.normalize(), HttpMethod.POST, request, Measurement.class);
                if (re.getStatusCode() != HttpStatus.CREATED) {
                    logger.error("Posting measurement with id '{}' to host '{}' returned code '{}'\nResponse body:\n{}",
                            measurement.getId(), host, re.getStatusCodeValue(), re.getBody());
                } else {
                    retryKey = false;
                }
            } catch (URISyntaxException e) {
                logger.error("could not create URI for host: {}", host, e);
            } catch (HttpServerErrorException e) {
                logger.error("Failed to post Measurement with id {} to host {}\nMessage: {}",
                        measurement.getId(), host, e.getResponseBodyAsString());
            } catch (RuntimeException re) {
                logger.error("syncAdd failed, check if token has expired!\nMeasurement: {}", measurement.toString(), re);
            }
            if (retryKey) {
                try {
                    measurementQueue.add(measurement);
                    measurementActionQueue.add("add");
                } catch(IllegalStateException e){
                    logger.info("No space is currently available in the Queue");
                }
            }
        }
    }

    public void syncUpdate(Measurement measurement) {
        retryKey = true;
        if (active) {
            HttpEntity<Measurement> request = new HttpEntity<>(measurement, createHeaders());
            logger.info("Updating measurement with id: {} - Host: {}", measurement.getId(), host);
            try {
                URI uri = new URI(host + "/measurement");
                ResponseEntity re = restTemplate.exchange(uri.normalize().toString(), HttpMethod.PUT, request, Measurement.class);
                if (re.getStatusCode() != HttpStatus.OK) {
                    logger.error("Updating measurement with id '{}' from host '{}' returned code '{}'\nResponse body:\n{}",
                            measurement.getId(), host, re.getStatusCodeValue(), re.getBody());
                } else {
                    retryKey = false;
                }
            } catch (URISyntaxException e) {
                logger.error("could not create URI for host: " + host, e);
            } catch (HttpServerErrorException e) {
                logger.error("Failed to update Measurement with id {} to host {}\nMessage: {}",
                        measurement.getId(), host, e.getResponseBodyAsString());
            } catch (RuntimeException re) {
                logger.error("syncUpdate failed, check if token has expired!\nMeasurement: {}", measurement.toString(), re);
            }
            if (retryKey) {
                try {
                    measurementQueue.add(measurement);
                    measurementActionQueue.add("update");
                } catch(IllegalStateException e){
                    logger.info("No space is currently available in the Queue");
                }
            }
        }
    }

    public void syncDelete(Measurement measurement) {
        retryKey = true;
        if (active) {
            HttpEntity<Measurement> request = new HttpEntity<>(createHeaders());
            logger.info("Deleting measurement with id: {} - Host: {}", measurement.getId(), host);
            try {
                URI uri = new URI(String.format("%s/measurement/%s", host, measurement.getId()));
                ResponseEntity re = restTemplate.exchange(uri.normalize().toString(), HttpMethod.DELETE, request, Void.class);
                if (re.getStatusCode() != HttpStatus.NO_CONTENT) {
                    logger.error("Deleting measurement with id '{}' from host '{}' returned code '{}'\nResponse body:\n{}",
                            measurement.getId(), host, re.getStatusCodeValue(), re.getBody());
                } else {
                    retryKey = false;
                }
            } catch (URISyntaxException e) {
                logger.error("could not create URI for host: " + host, e);
            } catch (HttpServerErrorException e) {
                logger.error("Failed to delete Measurement with id {} to host {}\nMessage: {}",
                        measurement.getId(), host, e.getResponseBodyAsString());
            } catch (RuntimeException re) {
                logger.error("syncDelete failed, check if token has expired!\nMeasurement: {}", measurement.toString(), re);
            }
            if (retryKey) {
                try {
                    measurementQueue.add(measurement);
                    measurementActionQueue.add("delete");
                } catch(IllegalStateException e){
                    logger.info("No space is currently available in the Queue");
                }
            }
        }
    }

    private String readFile(String filename) throws IOException {
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
