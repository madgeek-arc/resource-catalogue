package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("draftServiceManager")
public class DraftServiceManager extends ResourceManager<ServiceBundle> implements DraftResourceService<ServiceBundle> {

    private static final Logger logger = LoggerFactory.getLogger(DraftServiceManager.class);

    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final IdCreator idCreator;
    private final VocabularyService vocabularyService;
    private final ProviderService providerService;
    private final ProviderResourcesCommonMethods commonMethods;

    @Value("${catalogue.id}")
    private String catalogueId;

    public DraftServiceManager(ServiceBundleService<ServiceBundle> serviceBundleService,
                               IdCreator idCreator, @Lazy VocabularyService vocabularyService,
                               @Lazy ProviderService providerService,
                               ProviderResourcesCommonMethods commonMethods) {
        super(ServiceBundle.class);
        this.serviceBundleService = serviceBundleService;
        this.idCreator = idCreator;
        this.vocabularyService = vocabularyService;
        this.providerService = providerService;
        this.commonMethods = commonMethods;
    }

    @Override
    public String getResourceTypeName() {
        return "draft_service";
    }

    @Override
    public ServiceBundle add(ServiceBundle bundle, Authentication auth) {

        bundle.setId(idCreator.generate(getResourceTypeName()));

        logger.trace("Attempting to add a new Draft Service with id {}", bundle.getId());
        bundle.setMetadata(Metadata.updateMetadata(bundle.getMetadata(), AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase()));

        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.DRAFT.getKey(),
                LoggingInfo.ActionType.CREATED.getKey());
        loggingInfoList.add(loggingInfo);
        bundle.setLoggingInfo(loggingInfoList);

        bundle.getService().setCatalogueId(catalogueId);
        bundle.setActive(false);
        bundle.setDraft(true);

        super.add(bundle, auth);

        return bundle;
    }

    @Override
    public ServiceBundle update(ServiceBundle bundle, Authentication auth) {
        // get existing resource
        Resource existing = getDraftResource(bundle.getService().getId());
        // block catalogueId updates from Provider Admins
        bundle.getService().setCatalogueId(catalogueId);
        logger.trace("Attempting to update the Draft Service with id {}", bundle.getId());
        bundle.setMetadata(Metadata.updateMetadata(bundle.getMetadata(), AuthenticationInfo.getFullName(auth)));
        // save existing resource with new payload
        existing.setPayload(serialize(bundle));
        existing.setResourceType(getResourceType());
        resourceService.updateResource(existing);
        logger.debug("Updating Draft Service: {}", bundle);
        return bundle;
    }

    @Override
    public void delete(ServiceBundle bundle) {
        super.delete(bundle);
    }

    @Override
    public ServiceBundle transformToNonDraft(String id, Authentication auth) {
        ServiceBundle serviceBundle = this.get(id);
        return transformToNonDraft(serviceBundle, auth);
    }

    @Override
    public ServiceBundle transformToNonDraft(ServiceBundle bundle, Authentication auth) {
        logger.trace("Attempting to transform the Draft Service with id {} to Service", bundle.getId());
        serviceBundleService.validate(bundle);

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(bundle, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                LoggingInfo.ActionType.REGISTERED.getKey());
        loggingInfoList.add(loggingInfo);

        // set resource status according to Provider's templateStatus
        if (providerService.get(bundle.getService().getResourceOrganisation()).getTemplateStatus().equals("approved template")) {
            bundle.setStatus(vocabularyService.get("approved resource").getId());
            LoggingInfo loggingInfoApproved = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.APPROVED.getKey());
            loggingInfoList.add(loggingInfoApproved);
            bundle.setActive(true);
        } else {
            bundle.setStatus(vocabularyService.get("pending resource").getId());
        }
        bundle.setLoggingInfo(loggingInfoList);
        bundle.setLatestOnboardingInfo(loggingInfoList.get(loggingInfoList.size() - 1));

        bundle.setMetadata(Metadata.updateMetadata(bundle.getMetadata(), AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase()));
        bundle.setDraft(false);

        ResourceType serviceResourceType = resourceTypeService.getResourceType("service");
        Resource resource = getDraftResource(bundle.getId());
        resource.setResourceType(getResourceType());
        resourceService.changeResourceType(resource, serviceResourceType);

        try {
            bundle = serviceBundleService.update(bundle, auth);
        } catch (ResourceNotFoundException e) {
            logger.error(e.getMessage(), e);
        }

        return bundle;
    }

    @Override
    public Browsing<ServiceBundle> getMy(FacetFilter filter, Authentication auth) {
        List<ProviderBundle> providers = providerService.getMy(filter, auth).getResults();
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providers.stream().map(ProviderBundle::getId).toList());
        ff.setResourceType(getResourceTypeName());
        ff.setQuantity(1000);
        return this.getAll(ff, auth);
    }

    private Resource getDraftResource(String id) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("resource_internal_id = \"%s\" AND catalogue_id = \"%s\"", id, catalogueId),
                        getResourceTypeName());
        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults().get(0);
    }
}
