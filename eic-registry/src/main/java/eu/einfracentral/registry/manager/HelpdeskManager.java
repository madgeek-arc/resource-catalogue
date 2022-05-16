package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.ResourceService;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static eu.einfracentral.config.CacheConfig.*;

@org.springframework.stereotype.Service("helpdeskManager")
public class HelpdeskManager extends ResourceManager<HelpdeskBundle> implements ResourceService<HelpdeskBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(HelpdeskManager.class);
    private final InfraServiceService<InfraService, InfraService> infraServiceService;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final JmsTemplate jmsTopicTemplate;
    private final SecurityService securityService;

    @Autowired
    public HelpdeskManager(InfraServiceService<InfraService, InfraService> infraServiceService,
                           ProviderService<ProviderBundle, Authentication> providerService,
                           JmsTemplate jmsTopicTemplate, @Lazy SecurityService securityService) {
        super(HelpdeskBundle.class);
        this.infraServiceService = infraServiceService;
        this.providerService = providerService;
        this.jmsTopicTemplate = jmsTopicTemplate;
        this.securityService = securityService;
    }

    @Override
    public String getResourceType() {
        return "helpdesk";
    }

    @Override
    @CacheEvict(value = CACHE_HELPDESKS, allEntries = true)
    public HelpdeskBundle add(HelpdeskBundle helpdesk, Authentication auth) {

        // check if Service exists and if User belongs to Service's Provider Admins
        serviceConsistency(helpdesk.getHelpdesk().getServices(), helpdesk.getCatalogueId());

        helpdesk.setId(UUID.randomUUID().toString());
        logger.trace("User '{}' is attempting to add a new Helpdesk: {}", auth, helpdesk);

        helpdesk.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        loggingInfoList.add(loggingInfo);
        helpdesk.setLoggingInfo(loggingInfoList);
        helpdesk.setActive(true);
        // latestOnboardingInfo
        helpdesk.setLatestOnboardingInfo(loggingInfo);

        super.add(helpdesk, null);
        logger.debug("Adding Helpdesk: {}", helpdesk);

        //TODO: send emails
        jmsTopicTemplate.convertAndSend("helpdesk.create", helpdesk);

        return helpdesk;
    }

    @Override
    @CacheEvict(value = CACHE_HELPDESKS, allEntries = true)
    public HelpdeskBundle update(HelpdeskBundle helpdesk, Authentication auth) {

        logger.trace("User '{}' is attempting to update the Helpdesk with id '{}'", auth, helpdesk.getId());
        helpdesk.setMetadata(Metadata.updateMetadata(helpdesk.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo;
        loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.UPDATED.getKey());
        if (helpdesk.getLoggingInfo() != null) {
            loggingInfoList = helpdesk.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
        } else {
            loggingInfoList.add(loggingInfo);
        }
        helpdesk.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        helpdesk.setLatestUpdateInfo(loggingInfo);

        Resource existing = whereID(helpdesk.getId(), true);
        HelpdeskBundle ex = deserialize(existing);
        helpdesk.setActive(ex.isActive());
        existing.setPayload(serialize(helpdesk));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Helpdesk: {}", helpdesk);

        //TODO: send emails
        jmsTopicTemplate.convertAndSend("helpdesk.update", helpdesk);

        return helpdesk;
    }

    @CacheEvict(value = CACHE_HELPDESKS, allEntries = true)
    public void delete(HelpdeskBundle helpdesk, Authentication auth) {
        logger.trace("User '{}' is attempting to delete the Helpdesk with id '{}'", auth, helpdesk.getId());

        super.delete(helpdesk);
        logger.debug("Deleting Helpdesk: {}", helpdesk);

        //TODO: send emails
        jmsTopicTemplate.convertAndSend("helpdesk.delete", helpdesk);

    }

    public void serviceConsistency(List<String> serviceIds, String catalogueId){
        // check if Service exists
        for (String serviceId : serviceIds){
            try{
                infraServiceService.get(serviceId, catalogueId);
            } catch(ResourceNotFoundException e){
                throw new ValidationException(String.format("There is no Service with id '%s' in the '%s' Catalogue", serviceId, catalogueId));
            }
        }
    }
}
