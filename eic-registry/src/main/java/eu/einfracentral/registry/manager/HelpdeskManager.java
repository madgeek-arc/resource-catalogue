package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.HelpdeskService;
import eu.einfracentral.registry.service.ServiceBundleService;
import eu.einfracentral.registry.service.TrainingResourceService;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.ObjectUtils;
import eu.einfracentral.utils.ProviderResourcesCommonMethods;
import eu.einfracentral.utils.ResourceValidationUtils;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.SearchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service("helpdeskManager")
public class HelpdeskManager extends ResourceManager<HelpdeskBundle> implements HelpdeskService<HelpdeskBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(HelpdeskManager.class);
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceService;
    private final PublicHelpdeskManager publicHelpdeskManager;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final ProviderResourcesCommonMethods commonMethods;

    @Autowired
    public HelpdeskManager(ServiceBundleService<ServiceBundle> serviceBundleService,
                           TrainingResourceService<TrainingResourceBundle> trainingResourceService,
                           PublicHelpdeskManager publicHelpdeskManager,
                           @Lazy SecurityService securityService,
                           @Lazy RegistrationMailService registrationMailService,
                           ProviderResourcesCommonMethods commonMethods) {
        super(HelpdeskBundle.class);
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.publicHelpdeskManager = publicHelpdeskManager;
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
    public HelpdeskBundle update(HelpdeskBundle helpdeskBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Helpdesk with id '{}'", auth, helpdeskBundle.getId());

        HelpdeskBundle ret = ObjectUtils.clone(helpdeskBundle);
        Resource existingResource = whereID(ret.getId(), true);
        HelpdeskBundle existingHelpdesk = deserialize(existingResource);
        // check if there are actual changes in the Helpdesk
        if (ret.getHelpdesk().equals(existingHelpdesk.getHelpdesk())) {
            return ret;
        }

        validate(ret);
        ret.setMetadata(Metadata.updateMetadata(ret.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(ret, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey());
        loggingInfoList.add(loggingInfo);
        ret.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        ret.setLatestUpdateInfo(loggingInfo);
        ret.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        ret.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        ret.setActive(existingHelpdesk.isActive());
        existingResource.setPayload(serialize(ret));
        existingResource.setResourceType(resourceType);

        // block user from updating serviceId
        if (!ret.getHelpdesk().getServiceId().equals(existingHelpdesk.getHelpdesk().getServiceId()) && !securityService.hasRole(auth, "ROLE_ADMIN")){
            throw new ValidationException("You cannot change the Service Id with which this Helpdesk is related");
        }

        resourceService.updateResource(existingResource);
        logger.debug("Updating Helpdesk: {}", ret);

        registrationMailService.sendEmailsForHelpdeskExtension(ret, "Resource", "put");

        return ret;
    }

    @Override
    public void delete(HelpdeskBundle helpdesk) {
        super.delete(helpdesk);
        logger.debug("Deleting Helpdesk: {}", helpdesk);
    }

    public HelpdeskBundle createPublicResource(HelpdeskBundle helpdeskBundle, Authentication auth) {
        publicHelpdeskManager.add(helpdeskBundle, auth);
        return helpdeskBundle;
    }
}
