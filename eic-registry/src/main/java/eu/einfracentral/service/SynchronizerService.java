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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Service
public class SynchronizerService {

    private static final Logger logger = LogManager.getLogger(SynchronizerService.class);

    private RestTemplate restTemplate;
    private HttpHeaders headers;
    private URI url;
    private boolean active = false;
    private String host;
    private String token;

    // TODO: load token from file, to enable changing it on the fly

    @Autowired
    public SynchronizerService(@Value("${sync.host:}") String host, @Value("${sync.token:}") String token) throws URISyntaxException {
        this.host = host;
        this.token = token;
        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        if (!"".equals(token) && !"".equals(host)) {
            url = new URI(host + "/service");
            active = true;
        }
    }

    @Async
    public void syncAdd(InfraService infraService) {
        if (active) {
            HttpEntity<InfraService> request = new HttpEntity<>(infraService, headers);
            logger.info(String.format("Posting service with id: %s - Host: %s", infraService.getId(), host));
            restTemplate.postForObject(url.normalize(), request, InfraService.class);
        }
    }

    @Async
    public void syncUpdate(InfraService infraService) {
        if (active) {
            HttpEntity<InfraService> request = new HttpEntity<>(infraService, headers);
            logger.info(String.format("Updating service with id: %s - Host: %s", infraService.getId(), host));
            restTemplate.put(url.normalize().toString(), request, InfraService.class);
        }
    }

    @Async
    public void syncDelete(InfraService infraService) {
        if (active) {
            HttpEntity request = new HttpEntity<>(headers);
            try {
                logger.info(String.format("Deleting service with id: %s - Host: %s", infraService.getId(), host));
                URI uri = new URI(String.format("%s/infraService/%s/%s/", host, infraService.getId(), infraService.getVersion()));
                restTemplate.exchange(uri.normalize().toString(), HttpMethod.DELETE, request, Void.class);
            } catch (URISyntaxException e) {
                logger.error("Could not execute syncDelete method", e);
            }
        }
    }

    @Async
    public void syncAdd(Measurement measurement) {
        throw new UnsupportedOperationException("Method not implemented, yet");
    }

    @Async
    public void syncUpdate(Measurement measurement) {
        throw new UnsupportedOperationException("Method not implemented, yet");
    }

    @Async
    public void syncUpdateAll (List<Measurement> allMeasurements) {
        throw new UnsupportedOperationException("Method not implemented, yet");
    }

    @Async
    public void syncDelete(Measurement measurement) {
        throw new UnsupportedOperationException("Method not implemented, yet");
    }
}
