package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.HelpdeskService;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service("helpdeskManager")
public class HelpdeskManager extends ResourceManager<HelpdeskBundle> implements HelpdeskService<HelpdeskBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(HelpdeskManager.class);
    private final ResourceBundleService<ServiceBundle> serviceBundleService;
    private final ResourceBundleService<DatasourceBundle> datasourceBundleService;
    private final JmsTemplate jmsTopicTemplate;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;

    @Autowired
    public HelpdeskManager(ResourceBundleService<ServiceBundle> serviceBundleService,
                           ResourceBundleService<DatasourceBundle> datasourceBundleService,
                           JmsTemplate jmsTopicTemplate, @Lazy SecurityService securityService,
                           @Lazy RegistrationMailService registrationMailService) {
        super(HelpdeskBundle.class);
        this.serviceBundleService = serviceBundleService;
        this.datasourceBundleService = datasourceBundleService;
        this.jmsTopicTemplate = jmsTopicTemplate;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
    }

    @Override
    public String getResourceType() {
        return "helpdesk";
    }

    @Override
    public HelpdeskBundle add(HelpdeskBundle helpdesk, String resourceType, Authentication auth) {

        // check if Resource exists and if User belongs to Resource's Provider Admins
        if (resourceType.equals("service")){
            serviceConsistency(helpdesk.getHelpdesk().getServiceId(), helpdesk.getCatalogueId());
        } else if (resourceType.equals("datasource")){
            datasourceConsistency(helpdesk.getHelpdesk().getServiceId(), helpdesk.getCatalogueId());
        } else{
            throw new ValidationException("Field resourceType should be either 'service' or 'datasource'");
        }
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
        logger.info("Sending JMS with topic 'helpdesk.create'");
        jmsTopicTemplate.convertAndSend("helpdesk.create", helpdesk);

        return helpdesk;
    }

    @Override
    public HelpdeskBundle update(HelpdeskBundle helpdesk, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Helpdesk with id '{}'", auth, helpdesk.getId());

        Resource existing = whereID(helpdesk.getId(), true);
        HelpdeskBundle ex = deserialize(existing);
        // check if there are actual changes in the Helpdesk
        if (helpdesk.getHelpdesk().equals(ex.getHelpdesk())){
            throw new ValidationException("There are no changes in the Helpdesk", HttpStatus.OK);
        }

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

        helpdesk.setActive(ex.isActive());
        existing.setPayload(serialize(helpdesk));
        existing.setResourceType(resourceType);

        // block user from updating serviceId
        if (!helpdesk.getHelpdesk().getServiceId().equals(ex.getHelpdesk().getServiceId()) && !securityService.hasRole(auth, "ROLE_ADMIN")){
            throw new ValidationException("You cannot change the Service Id with which this Helpdesk is related");
        }

        resourceService.updateResource(existing);
        logger.debug("Updating Helpdesk: {}", helpdesk);

        registrationMailService.sendEmailsForHelpdeskExtension(helpdesk, "put");
        logger.info("Sending JMS with topic 'helpdesk.update'");
        jmsTopicTemplate.convertAndSend("helpdesk.update", helpdesk);

        return helpdesk;
    }

    public void delete(HelpdeskBundle helpdesk, Authentication auth) {
        logger.trace("User '{}' is attempting to delete the Helpdesk with id '{}'", auth, helpdesk.getId());

        super.delete(helpdesk);
        logger.debug("Deleting Helpdesk: {}", helpdesk);

        //TODO: send emails
        logger.info("Sending JMS with topic 'helpdesk.delete'");
        jmsTopicTemplate.convertAndSend("helpdesk.delete", helpdesk);

    }

    private void serviceConsistency(String serviceId, String catalogueId){
        ServiceBundle serviceBundle;
        // check if Service exists
        try{
            serviceBundle = serviceBundleService.get(serviceId, catalogueId);
            // check if Service is Public
            if (serviceBundle.getMetadata().isPublished()){
                throw new ValidationException("Please provide a Service ID with no catalogue prefix.");
            }
        } catch(ResourceNotFoundException e){
            throw new ValidationException(String.format("There is no Service with id '%s' in the '%s' Catalogue", serviceId, catalogueId));
        }
        // check if Service is Active + Approved
        if (!serviceBundle.isActive() || !serviceBundle.getStatus().equals("approved resource")){
            throw new ValidationException(String.format("Service with ID [%s] is not Approved and/or Active", serviceId));
        }
        // check if Service has already a Helpdesk registered
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<HelpdeskBundle> allHelpdesks = getAll(ff, null).getResults();
        for (HelpdeskBundle helpdesk : allHelpdesks){
            if (helpdesk.getHelpdesk().getServiceId().equals(serviceId) && helpdesk.getCatalogueId().equals(catalogueId)){
                throw new ValidationException(String.format("Service [%s] of the Catalogue [%s] has already a Helpdesk " +
                        "registered, with id: [%s]", serviceId, catalogueId, helpdesk.getId()));
            }
        }
    }

    private void datasourceConsistency(String serviceId, String catalogueId){
        DatasourceBundle datasourceBundle;
        // check if Resource exists
        try{
            datasourceBundle = datasourceBundleService.get(serviceId, catalogueId);
            // check if Datasource is Public
            if (datasourceBundle.getMetadata().isPublished()){
                throw new ValidationException("Please provide a Datasource ID with no catalogue prefix.");
            }
        } catch(ResourceNotFoundException e){
            throw new ValidationException(String.format("There is no Datasource with id '%s' in the '%s' Catalogue", serviceId, catalogueId));
        }
        // check if Datasource is Active + Approved
        if (!datasourceBundle.isActive() || !datasourceBundle.getStatus().equals("approved resource")){
            throw new ValidationException(String.format("Datasource with ID [%s] is not Approved and/or Active", serviceId));
        }
        // check if Datasource has already a Helpdesk registered
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<HelpdeskBundle> allHelpdesks = getAll(ff, null).getResults();
        for (HelpdeskBundle helpdesk : allHelpdesks){
            if (helpdesk.getHelpdesk().getServiceId().equals(serviceId) && helpdesk.getCatalogueId().equals(catalogueId)){
                throw new ValidationException(String.format("Datasource [%s] of the Catalogue [%s] has already a Helpdesk " +
                        "registered, with id: [%s]", serviceId, catalogueId, helpdesk.getId()));
            }
        }
    }
}
