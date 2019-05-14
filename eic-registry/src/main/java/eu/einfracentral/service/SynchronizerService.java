package eu.einfracentral.service;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Measurement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Service
public class SynchronizerService {

    private static final Logger logger = LogManager.getLogger(SynchronizerService.class);

    private RestTemplate restTemplate;
    private HttpHeaders headers;
    private boolean active = false;
    private String host;
    private String token;

    // TODO: load token from file, to enable changing it on the fly

    @Autowired
    public SynchronizerService(@Value("${sync.host:}") String host, @Value("${sync.token:}") String token) {
        this.host = host;
        this.token = token;
        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        if (!"".equals(token) && !"".equals(host)) {
            active = true;
        }
    }

    public void syncAdd(InfraService infraService) {
        if (active) {
            HttpEntity<InfraService> request = new HttpEntity<>(infraService, headers);
            logger.info(String.format("Posting service with id: %s - Host: %s", infraService.getId(), host));
            try {
                URI uri = new URI(host + "/service");
                restTemplate.postForObject(uri.normalize(), request, InfraService.class);
            } catch (Exception e) {
                logger.error("syncAdd failed, Service id: " + infraService.getId(), e);
            }
        }
    }

    public void syncUpdate(InfraService infraService) {
        if (active) {
            HttpEntity<InfraService> request = new HttpEntity<>(infraService, headers);
            logger.info(String.format("Updating service with id: %s - Host: %s", infraService.getId(), host));
            try {
                URI uri = new URI(host + "/service");
                restTemplate.put(uri.normalize().toString(), request, InfraService.class);
            } catch (Exception e) {
                logger.error("syncUpdate failed, Service id: " + infraService.getId(), e);
            }
        }
    }

    public void syncDelete(InfraService infraService) {
        if (active) {
            HttpEntity request = new HttpEntity<>(headers);
            logger.info(String.format("Deleting service with id: %s - Host: %s", infraService.getId(), host));
            try {
                URI uri = new URI(String.format("%s/infraService/%s/%s/", host, infraService.getId(), infraService.getVersion()));
                restTemplate.exchange(uri.normalize().toString(), HttpMethod.DELETE, request, Void.class);
            } catch (Exception e) {
                logger.error("syncDelete failed, Service id: " + infraService.getId(), e);
            }
        }
    }

    public void syncAdd(Measurement measurement) {
        if (active) {
            HttpEntity<Measurement> request = new HttpEntity<>(measurement, headers);
            logger.info(String.format("Posting measurement with id: %s - Host: %s", measurement.getId(), host));
            try {
                URI uri = new URI(host + "/measurement");
                restTemplate.postForObject(uri.normalize(), request, Measurement.class);
            } catch (Exception e) {
                logger.error("syncAdd failed, Measurement id: " + measurement.getId(), e);
            }
        }
    }

    public void syncUpdate(Measurement measurement) {
        if (active) {
            HttpEntity<Measurement> request = new HttpEntity<>(measurement, headers);
            logger.info(String.format("Updating measurement with id: %s - Host: %s", measurement.getId(), host));
            try {
                URI uri = new URI(host + "/measurement");
                restTemplate.put(uri.normalize().toString(), request, Measurement.class);
            } catch (Exception e) {
                logger.error("syncUpdate failed, Measurement id: " + measurement.getId(), e);
            }
        }
    }

    public void syncDelete(Measurement measurement) {
        if (active) {
            HttpEntity request = new HttpEntity<>(headers);
            logger.info(String.format("Deleting measurement with id: %s - Host: %s", measurement.getId(), host));
            try {
                URI uri = new URI(String.format("%s/measurement/%s", host, measurement.getId()));
                restTemplate.exchange(uri.normalize().toString(), HttpMethod.DELETE, request, Void.class);
            } catch (Exception e) {
                logger.error("syncDelete failed, Measurement id: " + measurement.getId(), e);
            }
        }
    }
}
