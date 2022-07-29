package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.ResourceService;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service("helpdeskManager")
public class HelpdeskManager extends ResourceManager<HelpdeskBundle> implements ResourceService<HelpdeskBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(HelpdeskManager.class);
    private final ResourceBundleService<ServiceBundle> resourceBundleService;
    private final JmsTemplate jmsTopicTemplate;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;

    @Autowired
    public HelpdeskManager(ResourceBundleService<ServiceBundle> resourceBundleService,
                           JmsTemplate jmsTopicTemplate, @Lazy SecurityService securityService,
                           @Lazy RegistrationMailService registrationMailService) {
        super(HelpdeskBundle.class);
        this.resourceBundleService = resourceBundleService;
        this.jmsTopicTemplate = jmsTopicTemplate;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
    }

    @Override
    public String getResourceType() {
        return "helpdesk";
    }

    @Override
    public HelpdeskBundle add(HelpdeskBundle helpdesk, Authentication auth) {

        // check if Service exists and if it has already a Helpdesk registered
        serviceConsistency(helpdesk.getHelpdesk().getServiceId(), helpdesk.getCatalogueId());
        validate(helpdesk);

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

        registrationMailService.sendEmailsForHelpdeskExtension(helpdesk, "post");
        jmsTopicTemplate.convertAndSend("helpdesk.create", helpdesk);

        return helpdesk;
    }

    @Override
    public HelpdeskBundle update(HelpdeskBundle helpdesk, Authentication auth) {

        logger.trace("User '{}' is attempting to update the Helpdesk with id '{}'", auth, helpdesk.getId());
        validate(helpdesk);
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

        // block user from updating serviceId
        if (!helpdesk.getHelpdesk().getServiceId().equals(ex.getHelpdesk().getServiceId())){
            throw new ValidationException("You cannot change the Service Id with which this Helpdesk is related");
        }

        resourceService.updateResource(existing);
        logger.debug("Updating Helpdesk: {}", helpdesk);

        registrationMailService.sendEmailsForHelpdeskExtension(helpdesk, "put");
        jmsTopicTemplate.convertAndSend("helpdesk.update", helpdesk);

        return helpdesk;
    }

    public void delete(HelpdeskBundle helpdesk, Authentication auth) {
        logger.trace("User '{}' is attempting to delete the Helpdesk with id '{}'", auth, helpdesk.getId());

        super.delete(helpdesk);
        logger.debug("Deleting Helpdesk: {}", helpdesk);

        //TODO: send emails
        jmsTopicTemplate.convertAndSend("helpdesk.delete", helpdesk);

    }

    public void serviceConsistency(String serviceId, String catalogueId){
        // check if Service exists
        try{
            resourceBundleService.get(serviceId, catalogueId);
        } catch(ResourceNotFoundException e){
            throw new ValidationException(String.format("There is no Service with id '%s' in the '%s' Catalogue", serviceId, catalogueId));
        }
        // check if Service has already a Helpdesk registered
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        List<HelpdeskBundle> allHelpdesks = getAll(ff, null).getResults();
        for (HelpdeskBundle helpdesk : allHelpdesks){
            if (helpdesk.getHelpdesk().getServiceId().equals(serviceId) && helpdesk.getCatalogueId().equals(catalogueId)){
                throw new ValidationException(String.format("Service [%s] of the Catalogue [%s] has already a Helpdesk " +
                        "registered, with id: [%s]", serviceId, catalogueId, helpdesk.getId()));
            }
        }
    }
}
