package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.HelpdeskService;
import eu.einfracentral.registry.service.ServiceBundleService;
import eu.einfracentral.registry.service.TrainingResourceService;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.ProviderResourcesCommonMethods;
import eu.einfracentral.utils.ResourceValidationUtils;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.SearchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service("helpdeskManager")
public class HelpdeskManager extends ResourceManager<HelpdeskBundle> implements HelpdeskService<HelpdeskBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(HelpdeskManager.class);
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceService;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final ProviderResourcesCommonMethods commonMethods;

    @Autowired
    public HelpdeskManager(ServiceBundleService<ServiceBundle> serviceBundleService,
                           TrainingResourceService<TrainingResourceBundle> trainingResourceService,
                           @Lazy SecurityService securityService,
                           @Lazy RegistrationMailService registrationMailService,
                           ProviderResourcesCommonMethods commonMethods) {
        super(HelpdeskBundle.class);
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.commonMethods = commonMethods;
    }

    @Override
    public String getResourceType() {
        return "helpdesk";
    }

    @Override
    public HelpdeskBundle validate(HelpdeskBundle helpdeskBundle, String resourceType) {
        String resourceId = helpdeskBundle.getHelpdesk().getServiceId();
        String catalogueId = helpdeskBundle.getCatalogueId();

        HelpdeskBundle existingHelpdesk = get(resourceId, catalogueId);
        if (existingHelpdesk != null) {
            throw new ValidationException(String.format("Resource [%s] of the Catalogue [%s] has already a Helpdesk " +
                    "registered, with id: [%s]", resourceId, catalogueId, existingHelpdesk.getId()));
        }

        // check if Resource exists and if User belongs to Resource's Provider Admins
        if (resourceType.equals("service")){
            ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(resourceId, catalogueId, serviceBundleService, resourceType);
        } else if (resourceType.equals("training_resource")){
            ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(resourceId, catalogueId, trainingResourceService, resourceType);
        } else{
            throw new ValidationException("Field resourceType should be either 'service' or 'training_resource'");
        }
        return super.validate(helpdeskBundle);
    }

    @Override
    public HelpdeskBundle add(HelpdeskBundle helpdesk, String resourceType, Authentication auth) {
        validate(helpdesk, resourceType);

        helpdesk.setId(UUID.randomUUID().toString());
        logger.trace("User '{}' is attempting to add a new Helpdesk: {}", auth, helpdesk);

        helpdesk.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(helpdesk, auth);
        helpdesk.setLoggingInfo(loggingInfoList);
        helpdesk.setActive(true);
        // latestOnboardingInfo
        helpdesk.setLatestOnboardingInfo(loggingInfoList.get(0));

        super.add(helpdesk, null);
        logger.debug("Adding Helpdesk: {}", helpdesk);

        registrationMailService.sendEmailsForHelpdeskExtension(helpdesk, resourceType, "post");

        return helpdesk;
    }

    @Override
    public HelpdeskBundle get(String serviceId, String catalogueId) {
        Resource res = where(false, new SearchService.KeyValue("service_id", serviceId), new SearchService.KeyValue("catalogue_id", catalogueId));
        return res != null ? deserialize(res) : null;
    }

    @Override
    public HelpdeskBundle update(HelpdeskBundle helpdesk, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Helpdesk with id '{}'", auth, helpdesk.getId());

        Resource existing = whereID(helpdesk.getId(), true);
        HelpdeskBundle ex = deserialize(existing);
        // check if there are actual changes in the Helpdesk
        if (helpdesk.getHelpdesk().equals(ex.getHelpdesk())) {
            return helpdesk;
        }

        validate(helpdesk);
        helpdesk.setMetadata(Metadata.updateMetadata(helpdesk.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(helpdesk, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey());
        loggingInfoList.add(loggingInfo);
        helpdesk.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        helpdesk.setLatestUpdateInfo(loggingInfo);
        helpdesk.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        helpdesk.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        helpdesk.setActive(ex.isActive());
        existing.setPayload(serialize(helpdesk));
        existing.setResourceType(resourceType);

        // block user from updating serviceId
        if (!helpdesk.getHelpdesk().getServiceId().equals(ex.getHelpdesk().getServiceId()) && !securityService.hasRole(auth, "ROLE_ADMIN")){
            throw new ValidationException("You cannot change the Service Id with which this Helpdesk is related");
        }

        resourceService.updateResource(existing);
        logger.debug("Updating Helpdesk: {}", helpdesk);

        registrationMailService.sendEmailsForHelpdeskExtension(helpdesk, "Resource", "put");

        return helpdesk;
    }

    @Override
    public void delete(HelpdeskBundle helpdesk) {
        super.delete(helpdesk);
        logger.debug("Deleting Helpdesk: {}", helpdesk);
    }
}
