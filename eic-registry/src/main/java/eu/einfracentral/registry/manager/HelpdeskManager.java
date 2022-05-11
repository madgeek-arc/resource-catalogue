package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Helpdesk;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.HelpdeskService;
import eu.einfracentral.registry.service.InfraServiceService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

import static eu.einfracentral.config.CacheConfig.*;

@org.springframework.stereotype.Service("helpdeskManager")
public class HelpdeskManager extends ResourceManager<Helpdesk> implements HelpdeskService<Helpdesk, Authentication> {

    private static final Logger logger = LogManager.getLogger(HelpdeskManager.class);
    private final InfraServiceService<InfraService, InfraService> infraServiceService;

    public HelpdeskManager(InfraServiceService<InfraService, InfraService> infraServiceService) {
        super(Helpdesk.class);
        this.infraServiceService = infraServiceService;
    }

    @Override
    public String getResourceType() {
        return "helpdesk";
    }

    @Override
    @Cacheable(value = CACHE_HELPDESKS)
    public Helpdesk get(String id) {
        return super.get(id);
    }

    @Override
    @CacheEvict(value = CACHE_HELPDESKS, allEntries = true)
    public Helpdesk add(Helpdesk helpdesk, Authentication auth) {

        // check if Service exists
        serviceConsistency(helpdesk.getServices());

        helpdesk.setId(UUID.randomUUID().toString());
        logger.trace("User '{}' is attempting to add a new Helpdesk: {}", auth, helpdesk);
        //TODO: metadata
        //TODO: loggingInfo

        super.add(helpdesk, null);
        logger.debug("Adding Helpdesk: {}", helpdesk);

        //TODO: send emails

        //TODO: jms?

        return helpdesk;
    }

    @Override
    @CacheEvict(value = CACHE_HELPDESKS, allEntries = true)
    public Helpdesk update(Helpdesk helpdesk, Authentication auth) {

        helpdesk.setId(UUID.randomUUID().toString());
        logger.trace("User '{}' is attempting to update the Helpdesk with id '{}'", auth, helpdesk.getId());
        //TODO: metadata
        //TODO: loggingInfo

        super.update(helpdesk, auth);
        logger.debug("Updating Helpdesk: {}", helpdesk);

        //TODO: send emails

        //TODO: jms?

        return helpdesk;
    }

    @CacheEvict(value = CACHE_HELPDESKS, allEntries = true)
    public void delete(Helpdesk helpdesk, Authentication auth) {
        logger.trace("User '{}' is attempting to delete the Helpdesk with id '{}'", auth, helpdesk.getId());

        super.delete(helpdesk);
        logger.debug("Deleting Helpdesk: {}", helpdesk);

        //TODO: send emails

        //TODO: jms?

    }

    public void serviceConsistency(List<String> serviceIds){
        for (String serviceId : serviceIds){
            try{
                infraServiceService.get(serviceId);
            } catch(ResourceNotFoundException e){
                //TODO: check if a Monitoring can belong to different than EOSC Catalogues (catalogueId)
                throw new ValidationException(String.format("There is no Service with id '%s' in the EOSC Catalogue", serviceId));
            }
        }
    }
}
