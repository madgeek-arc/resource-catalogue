package eu.einfracentral.service;

import eu.einfracentral.domain.InfraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class SynchronizerService {

    private RestTemplate restTemplate;
    private HttpHeaders headers;
    private URI url;
    private boolean active = false;
    private String host;
    private String token;

    // TODO: load token from file, to enable changing it on the fly

    @Autowired
    public SynchronizerService(@Value("${sync.host:localhost}") String host, @Value("${sync.token:noToken}") String token) throws URISyntaxException {
        this.host = host;
        this.token = token;
        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        url = new URI(host + "/service");
        if (!token.equals("noToken")) {
            active = true;
        }
    }

    @Async
    public void syncAdd(InfraService infraService) {
        if (active) {
            HttpEntity<InfraService> request = new HttpEntity<>(infraService, headers);
            restTemplate.postForObject(url.normalize(), request, InfraService.class);
        }
    }

    @Async
    public void syncUpdate(InfraService infraService) {
        if (active) {
            HttpEntity<InfraService> request = new HttpEntity<>(infraService, headers);
            restTemplate.put(url.normalize().toString(), request, InfraService.class);
        }
    }

    @Async
    public void syncDelete(InfraService infraService) {
        if (active) {
            HttpEntity<String> request = new HttpEntity<>(infraService.getId(), headers);
            // FIXME
            try {
                restTemplate.delete(new URI(host + "/infraService").normalize().toString(), request, String.class);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }
}
