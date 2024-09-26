package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.Metadata;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("draftProviderManager")
public class DraftProviderManager extends ResourceManager<ProviderBundle> implements DraftResourceService<ProviderBundle> {

    private static final Logger logger = LoggerFactory.getLogger(DraftProviderManager.class);

    private final ProviderService providerManager;
    private final IdCreator idCreator;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final ProviderResourcesCommonMethods commonMethods;

    @Value("${catalogue.id}")
    private String catalogueId;

    public DraftProviderManager(ProviderService providerManager,
                                IdCreator idCreator, @Lazy RegistrationMailService registrationMailService,
                                @Lazy VocabularyService vocabularyService,
                                ProviderResourcesCommonMethods commonMethods) {
        super(ProviderBundle.class);
        this.providerManager = providerManager;
        this.idCreator = idCreator;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.commonMethods = commonMethods;
    }


    @Override
    public String getResourceType() {
        return "draft_provider";
    }

    @Override
    public ProviderBundle add(ProviderBundle bundle, Authentication auth) {

        bundle.setId(idCreator.generate(getResourceType()));
        commonMethods.addAuthenticatedUser(bundle.getProvider(), auth);

        logger.trace("Attempting to add a new Draft Provider: {}", bundle);
        bundle.setMetadata(Metadata.updateMetadata(bundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));

        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.DRAFT.getKey(),
                LoggingInfo.ActionType.CREATED.getKey());
        loggingInfoList.add(loggingInfo);
        bundle.setLoggingInfo(loggingInfoList);

        bundle.getProvider().setCatalogueId(catalogueId);
        bundle.setActive(false);
        bundle.setDraft(true);

        super.add(bundle, auth);

        return bundle;
    }

    @Override
    public ProviderBundle update(ProviderBundle bundle, Authentication auth) {
        // get existing resource
        Resource existing = getDraftResource(bundle.getId());
        // block catalogueId updates from Provider Admins
        bundle.getProvider().setCatalogueId(catalogueId);
        logger.trace("Attempting to update the Draft Provider: {}", bundle);
        bundle.setMetadata(Metadata.updateMetadata(bundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        // save existing resource with new payload
        existing.setPayload(serialize(bundle));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Draft Provider: {}", bundle);
        return bundle;
    }

    @Override
    public void delete(ProviderBundle bundle) {
        super.delete(bundle);
    }

    @Override
    public ProviderBundle transformToNonDraft(String id, Authentication auth) {
        ProviderBundle providerBundle = get(id);
        return transformToNonDraft(providerBundle, auth);
    }

    @Override
    public ProviderBundle transformToNonDraft(ProviderBundle bundle, Authentication auth) {
        logger.trace("Attempting to transform the Draft Provider with id '{}' to Provider", bundle.getId());
        providerManager.validate(bundle);

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(bundle, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                LoggingInfo.ActionType.REGISTERED.getKey());
        loggingInfoList.add(loggingInfo);
        bundle.setLoggingInfo(loggingInfoList);
        bundle.setLatestOnboardingInfo(loggingInfo);

        // update providerStatus
        bundle.setStatus(vocabularyService.get("pending provider").getId());
        bundle.setTemplateStatus(vocabularyService.get("no template status").getId());

        bundle.setMetadata(Metadata.updateMetadata(bundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        bundle.setDraft(false);

        ResourceType providerResourceType = resourceTypeService.getResourceType("provider");
        Resource resource = getDraftResource(bundle.getId());
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, providerResourceType);

        try {
            bundle = providerManager.update(bundle, auth);
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
        }

        registrationMailService.sendEmailsToNewlyAddedAdmins(bundle, null);
        return bundle;
    }

    public List<ProviderBundle> getMy(Authentication auth) {
        if (auth == null) {
            return new ArrayList<>();
        }
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("users", User.of(auth).getEmail());
        ff.addOrderBy("name", "asc");
        return super.getAll(ff, auth).getResults();
    }

    private Resource getDraftResource(String id) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("resource_internal_id = \"%s\" AND catalogue_id = \"%s\"", id, catalogueId),
                        resourceType.getName());
        assert resources != null;
        return resources.getTotal() == 0 ? null : resources.getResults().get(0);
    }
}
