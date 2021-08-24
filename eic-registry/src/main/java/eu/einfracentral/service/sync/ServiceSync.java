package eu.einfracentral.service.sync;

import eu.einfracentral.domain.InfraService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class ServiceSync extends AbstractSyncService<InfraService> {

    private static final Logger logger = LogManager.getLogger(ServiceSync.class);

    @Autowired
    public ServiceSync(@Value("${sync.host:}") String host, @Value("${sync.token.filepath:}") String filename) {
        super(host, filename);
    }

    @Override
    protected String getController() {
        return "/infraService";
    }

    @Override
    public boolean filterOut(InfraService infraService, InfraService previous) {
        return infraService.getService().equals(previous.getService());
    }

    @Override
    public void syncDelete(InfraService infraService) {
        boolean retryKey = true;
        if (active) {
            HttpEntity<InfraService> request = new HttpEntity<>(this.createHeaders());
            logger.info("Deleting {} with id: {} - Host: {}", infraService.getClass(), infraService.getId(), host);
            try {
                URI uri = new URI(String.format("%s/%s/%s/%s", host, getController(), infraService.getId(), infraService.getService().getVersion()));
                ResponseEntity<?> re = restTemplate.exchange(uri.normalize().toString(), HttpMethod.DELETE, request, Void.class);
                if (re.getStatusCode() != HttpStatus.NO_CONTENT) {
                    logger.error("Deleting service with id '{}' from host '{}' returned code '{}'\nResponse body:\n{}",
                            infraService.getService().getId(), host, re.getStatusCodeValue(), re.getBody());
                } else {
                    retryKey = false;
                }
            } catch (URISyntaxException e) {
                logger.error("could not create URI for host: {}", host, e);
            } catch (HttpServerErrorException e) {
                logger.error("Failed to delete Service with id {} to host {}\nMessage: {}",
                        infraService.getService().getId(), host, e.getResponseBodyAsString());
            } catch (RuntimeException re) {
                logger.error("syncDelete failed, check if token has expired!\nService: {}", infraService.toString(), re);
            }
            if (retryKey) {
                try {
                    getQueue().add(Pair.with(infraService, "delete"));
//                    getQueue().add(infraService);
//                    getActionQueue().add("delete");
                } catch (IllegalStateException e) {
                    logger.info("No space is currently available in the Queue");
                }
            }
        }
    }
}
