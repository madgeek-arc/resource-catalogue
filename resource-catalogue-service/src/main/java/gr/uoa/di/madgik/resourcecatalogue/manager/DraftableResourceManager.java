package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceAlreadyExistsException;
import gr.uoa.di.madgik.resourcecatalogue.service.DraftResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.IdCreator;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;


@Component
public abstract class DraftableResourceManager<T extends Bundle<?>> extends ResourceManager<T> implements ResourceService<T>, DraftResourceService<T> {

    private static final Logger logger = LoggerFactory.getLogger(DraftableResourceManager.class);

    @Autowired
    protected GenericResourceService genericResourceService;
    @Autowired
    protected IdCreator idCreator;
    @Autowired
    protected ProviderResourcesCommonMethods commonMethods;

    public DraftableResourceManager(Class<T> typeParameterClass) {
        super(typeParameterClass);
    }

    @Override
    public T transformToNonDraft(T t, Authentication auth) {
        logger.trace("Attempting to transform the Draft Provider with id '{}' to Active", t.getId());
        this.validate(t);
        if (this.exists(t.getId())) {
            throw new ResourceAlreadyExistsException(String.format("Provider with id = '%s' already exists!", t.getId()));
        }

//        // update loggingInfo
//        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(t, auth);
//        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
//                LoggingInfo.ActionType.REGISTERED.getKey());
//        loggingInfoList.add(loggingInfo);
//        t.setLoggingInfo(loggingInfoList);
//
//        // latestOnboardInfo
//        t.setLatestOnboardingInfo(loggingInfo);
//
//        t.setMetadata(Metadata.updateMetadata(t.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
//
//
//        // latestOnboardInfo
//        t.setLatestOnboardingInfo(loggingInfo);
//
//        t.setMetadata(Metadata.updateMetadata(t.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));

        ResourceType providerResourceType = resourceTypeService.getResourceType("provider");
        Resource resource = genericResourceService.searchResource("getDraftResourceType()", t.getId(), true);
        resource.setResourceTypeName("getDraftResourceType()");
        resourceService.changeResourceType(resource, providerResourceType);

        return t;
    }
}
