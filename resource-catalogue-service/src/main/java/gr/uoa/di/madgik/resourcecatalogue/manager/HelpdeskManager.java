package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.HelpdeskBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.utils.ResourceValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;

import java.util.List;

@org.springframework.stereotype.Service("helpdeskManager")
public class HelpdeskManager extends ResourceManager<HelpdeskBundle> implements HelpdeskService {

    private static final Logger logger = LoggerFactory.getLogger(HelpdeskManager.class);
    private final ServiceBundleService serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final PublicHelpdeskManager publicHelpdeskManager;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final IdCreator idCreator;

    @Autowired
    public HelpdeskManager(ServiceBundleService serviceBundleService,
                           TrainingResourceService trainingResourceService,
                           PublicHelpdeskManager publicHelpdeskManager,
                           @Lazy SecurityService securityService,
                           @Lazy RegistrationMailService registrationMailService,
                           ProviderResourcesCommonMethods commonMethods,
                           IdCreator idCreator) {
        super(HelpdeskBundle.class);
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.publicHelpdeskManager = publicHelpdeskManager;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.commonMethods = commonMethods;
        this.idCreator = idCreator;
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
        if (resourceType.equals("service")) {
            ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(resourceId, catalogueId, serviceBundleService, resourceType);
        } else if (resourceType.equals("training_resource")) {
            ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(resourceId, catalogueId, trainingResourceService, resourceType);
        } else {
            throw new ValidationException("Field resourceType should be either 'service' or 'training_resource'");
        }
        return super.validate(helpdeskBundle);
    }

    @Override
    public HelpdeskBundle add(HelpdeskBundle helpdesk, String resourceType, Authentication auth) {
        validate(helpdesk, resourceType);

        helpdesk.setId(idCreator.generate(getResourceType()));
        logger.trace("Attempting to add a new Helpdesk: {}", helpdesk);

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
        logger.trace("Attempting to update the Helpdesk with id '{}'", helpdeskBundle.getId());

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
        if (!ret.getHelpdesk().getServiceId().equals(existingHelpdesk.getHelpdesk().getServiceId()) && !securityService.hasRole(auth, "ROLE_ADMIN")) {
            throw new ValidationException("You cannot change the Service Id with which this Helpdesk is related");
        }

        resourceService.updateResource(existingResource);
        logger.debug("Updating Helpdesk: {}", ret);

        registrationMailService.sendEmailsForHelpdeskExtension(ret, "Resource", "put");

        return ret;
    }

    public void updateBundle(HelpdeskBundle helpdeskBundle, Authentication auth) {
        logger.trace("Attempting to update the Helpdesk: {}", helpdeskBundle);

        Resource existing = getResource(helpdeskBundle.getId());
        if (existing == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not update Helpdesk with id '%s' because it does not exist",
                            helpdeskBundle.getId()));
        }

        existing.setPayload(serialize(helpdeskBundle));
        existing.setResourceType(resourceType);

        resourceService.updateResource(existing);
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
