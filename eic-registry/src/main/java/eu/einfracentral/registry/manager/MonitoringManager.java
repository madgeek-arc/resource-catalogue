package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.domain.Monitoring;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.MonitoringService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static eu.einfracentral.config.CacheConfig.CACHE_MONITORINGS;

@org.springframework.stereotype.Service("monitoringManager")
public class MonitoringManager extends ResourceManager<Monitoring> implements MonitoringService<Monitoring, Authentication> {

    private static final Logger logger = LogManager.getLogger(MonitoringManager.class);
    private final InfraServiceService<InfraService, InfraService> infraServiceService;


    public MonitoringManager(InfraServiceService<InfraService, InfraService> infraServiceService) {
        super(Monitoring.class);
        this.infraServiceService = infraServiceService;
    }

    @Override
    public String getResourceType() {
        return "monitoring";
    }

    @Override
    @Cacheable(value = CACHE_MONITORINGS)
    public Monitoring get(String id) {
        return super.get(id);
    }

    @Override
    @CacheEvict(value = CACHE_MONITORINGS, allEntries = true)
    public Monitoring add(Monitoring monitoring, Authentication auth) {

        // check if Service exists
        serviceConsistency(monitoring.getService(), monitoring.getCatalogueId());

        monitoring.setId(UUID.randomUUID().toString());
        logger.trace("User '{}' is attempting to add a new Monitoring: {}", auth, monitoring);
        //TODO: metadata
        //TODO: loggingInfo

        super.add(monitoring, auth);
        logger.debug("Adding Monitoring: {}", monitoring);

        //TODO: send emails

        //TODO: jms?

        return monitoring;
    }

    @Override
    @CacheEvict(value = CACHE_MONITORINGS, allEntries = true)
    public Monitoring update(Monitoring monitoring, Authentication auth) {

        logger.trace("User '{}' is attempting to update the Monitoring with id '{}'", auth, monitoring.getId());
        //TODO: metadata
        //TODO: loggingInfo

        super.update(monitoring, null);
        logger.debug("Updating Monitoring: {}", monitoring);

        //TODO: send emails

        //TODO: jms?

        return monitoring;
    }

    @CacheEvict(value = CACHE_MONITORINGS, allEntries = true)
    public void delete(Monitoring monitoring, Authentication auth) {
        logger.trace("User '{}' is attempting to delete the Monitoring with id '{}'", auth, monitoring.getId());

        super.delete(monitoring);
        logger.debug("Deleting Monitoring: {}", monitoring);

        //TODO: send emails

        //TODO: jms?

    }

    public void serviceConsistency(String serviceId, String catalogueId){
        try{
            infraServiceService.get(serviceId, catalogueId);
        } catch(ResourceNotFoundException e){
            throw new ValidationException(String.format("There is no Service with id '%s' in the '%s' Catalogue", serviceId, catalogueId));
        }
    }
}
