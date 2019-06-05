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

@Service
public class SynchronizerService {

    private static final Logger logger = LogManager.getLogger(SynchronizerService.class);

    private RestTemplate restTemplate;
    private boolean active = false;
    private String host;
    private String filename;

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
    }

    private HttpHeaders createHeaders() {
        String token;
        HttpHeaders headers = new HttpHeaders();
        try {
            token = readFile(filename);
            headers.add("Authorization", "Bearer " + token);
        } catch (IOException e) {
            logger.error(String.format("Could not read file '%s' containing the synchronization token", filename), e);
        }

        return headers;
    }

    public void syncAdd(InfraService infraService) {
        if (active) {
            HttpEntity<InfraService> request = new HttpEntity<>(infraService, createHeaders());
            logger.info(String.format("Posting service with id: %s - Host: %s", infraService.getId(), host));
            try {
                URI uri = new URI(host + "/infraService");
                ResponseEntity re = restTemplate.exchange(uri.normalize(), HttpMethod.POST, request, InfraService.class);
                if (re.getStatusCode() != HttpStatus.CREATED) {
                    logger.error(String.format("Adding service with id '%s' from host '%s' returned code '%d'%nResponse body:%n%s",
                            infraService.getId(), host, re.getStatusCodeValue(), re.getBody()));
                }
            } catch (URISyntaxException e) {
                logger.error("could not create URI for host: " + host, e);
            } catch (HttpServerErrorException e) {
                logger.error(String.format("Failed to post Service with id %s to host %s%nMessage: %s",
                        infraService.getId(), host, e.getResponseBodyAsString()));
            } catch (RuntimeException re) {
                logger.error("syncAdd failed, Service id: " + infraService.getId(), re);
            }
        }
    }

    public void syncUpdate(InfraService infraService) {
        if (active) {
            HttpEntity<InfraService> request = new HttpEntity<>(infraService, createHeaders());
            logger.info(String.format("Updating service with id: %s - Host: %s", infraService.getId(), host));
            try {
                URI uri = new URI(host + "/infraService");
                ResponseEntity re = restTemplate.exchange(uri.normalize().toString(), HttpMethod.PUT, request, InfraService.class);
                if (re.getStatusCode() != HttpStatus.OK) {
                    logger.error(String.format("Updating service with id '%s' from host '%s' returned code '%d'%nResponse body:%n%s",
                            infraService.getId(), host, re.getStatusCodeValue(), re.getBody()));
                }
            } catch (URISyntaxException e) {
                logger.error("could not create URI for host: " + host, e);
            } catch (HttpServerErrorException e) {
                logger.error(String.format("Failed to update Service with id %s to host %s%nMessage: %s",
                        infraService.getId(), host, e.getResponseBodyAsString()));
            } catch (RuntimeException re) {
                logger.error("syncUpdate failed, Service id: " + infraService.getId(), re);
            }
        }
    }

    public void syncDelete(InfraService infraService) {
        if (active) {
            HttpEntity<InfraService> request = new HttpEntity<>(createHeaders());
            logger.info(String.format("Deleting service with id: %s - Host: %s", infraService.getId(), host));
            try {
                URI uri = new URI(String.format("%s/infraService/%s/%s", host, infraService.getId(), infraService.getVersion()));
                ResponseEntity re = restTemplate.exchange(uri.normalize().toString(), HttpMethod.DELETE, request, Void.class);
                if (re.getStatusCode() != HttpStatus.NO_CONTENT) {
                    logger.error(String.format("Deleting service with id '%s' from host '%s' returned code '%d'%nResponse body:%n%s",
                            infraService.getId(), host, re.getStatusCodeValue(), re.getBody()));
                }
            } catch (URISyntaxException e) {
                logger.error("could not create URI for host: " + host, e);
            } catch (HttpServerErrorException e) {
                logger.error(String.format("Failed to delete Service with id %s to host %s%nMessage: %s",
                        infraService.getId(), host, e.getResponseBodyAsString()));
            } catch (RuntimeException re) {
                logger.error("syncDelete failed, Service id: " + infraService.getId(), re);
            }
        }
    }

    public void syncAdd(Measurement measurement) {
        if (active) {
            HttpEntity<Measurement> request = new HttpEntity<>(measurement, createHeaders());
            logger.info(String.format("Posting measurement with id: %s - Host: %s", measurement.getId(), host));
            try {
                URI uri = new URI(host + "/measurement");
                ResponseEntity re = restTemplate.exchange(uri.normalize(), HttpMethod.POST, request, Measurement.class);
                if (re.getStatusCode() != HttpStatus.CREATED) {
                    logger.error(String.format("Adding measurement with id '%s' from host '%s' returned code '%d'%nResponse body:%n%s",
                            measurement.getId(), host, re.getStatusCodeValue(), re.getBody()));
                }
            } catch (URISyntaxException e) {
                logger.error("could not create URI for host: " + host, e);
            } catch (HttpServerErrorException e) {
                logger.error(String.format("Failed to post Measurement with id %s to host %s%nMessage: %s",
                        measurement.getId(), host, e.getResponseBodyAsString()));
            } catch (RuntimeException re) {
                logger.error("syncAdd failed, Measurement id: " + measurement.getId(), re);
            }
        }
    }

    public void syncUpdate(Measurement measurement) {
        if (active) {
            HttpEntity<Measurement> request = new HttpEntity<>(measurement, createHeaders());
            logger.info(String.format("Updating measurement with id: %s - Host: %s", measurement.getId(), host));
            try {
                URI uri = new URI(host + "/measurement");
                ResponseEntity re = restTemplate.exchange(uri.normalize().toString(), HttpMethod.PUT, request, Measurement.class);
                if (re.getStatusCode() != HttpStatus.OK) {
                    logger.error(String.format("Updating measurement with id '%s' from host '%s' returned code '%d'%nResponse body:%n%s",
                            measurement.getId(), host, re.getStatusCodeValue(), re.getBody()));
                }
            } catch (URISyntaxException e) {
                logger.error("could not create URI for host: " + host, e);
            } catch (HttpServerErrorException e) {
                logger.error(String.format("Failed to update Measurement with id %s to host %s%nMessage: %s",
                        measurement.getId(), host, e.getResponseBodyAsString()));
            } catch (RuntimeException re) {
                logger.error("syncUpdate failed, Measurement id: " + measurement.getId(), re);
            }
        }
    }

    public void syncDelete(Measurement measurement) {
        if (active) {
            HttpEntity<Measurement> request = new HttpEntity<>(createHeaders());
            logger.info(String.format("Deleting measurement with id: %s - Host: %s", measurement.getId(), host));
            try {
                URI uri = new URI(String.format("%s/measurement/%s", host, measurement.getId()));
                ResponseEntity re = restTemplate.exchange(uri.normalize().toString(), HttpMethod.DELETE, request, Void.class);
                if (re.getStatusCode() != HttpStatus.NO_CONTENT) {
                    logger.error(String.format("Deleting measurement with id '%s' from host '%s' returned code '%d'%nResponse body:%n%s",
                            measurement.getId(), host, re.getStatusCodeValue(), re.getBody()));
                }
            } catch (URISyntaxException e) {
                logger.error("could not create URI for host: " + host, e);
            } catch (HttpServerErrorException e) {
                logger.error(String.format("Failed to delete Measurement with id %s to host %s%nMessage: %s",
                        measurement.getId(), host, e.getResponseBodyAsString()));
            } catch (RuntimeException re) {
                logger.error("syncDelete failed, Measurement id: " + measurement.getId(), re);
            }
        }
    }
}
