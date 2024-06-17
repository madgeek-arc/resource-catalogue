package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceAlreadyExistsException;
import gr.uoa.di.madgik.resourcecatalogue.service.DraftResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.IdCreator;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static gr.uoa.di.madgik.resourcecatalogue.config.Properties.Cache.CACHE_PROVIDERS;

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
    @Cacheable(value = CACHE_PROVIDERS)
    public T getDraft(String id, Authentication authentication) {
        T provider = genericResourceService.get(getDraftResourceType(), id);
        if (provider == null) {
            throw new gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException(
                    String.format("Could not find draft provider with id: %s", id));
        }
        return provider;
    }

    @Override
    public Browsing<T> getAllDrafts(FacetFilter facetFilter, Authentication authentication) {
        return genericResourceService.getResults(facetFilter);
    }

    @Override
    public List<T> getMyDrafts(Authentication auth) {
        if (auth == null) {
            return new ArrayList<>();
        }
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(getDraftResourceType());
        ff.setQuantity(maxQuantity);
        ff.addFilter("users", User.of(auth).getEmail());
        ff.addOrderBy("name", "asc");
        return (List) genericResourceService.getResults(ff).getResults();
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public T addDraft(T t, Authentication auth) {
        t.setId(idCreator.generate(getDraftResourceType()));

//        logger.trace("Attempting to add a new Draft Provider: {}", t);
//        t.setMetadata(Metadata.updateMetadata(t.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
//
//        List<LoggingInfo> loggingInfoList = new ArrayList<>();
//        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.DRAFT.getKey(),
//                LoggingInfo.ActionType.CREATED.getKey());
//        loggingInfoList.add(loggingInfo);
//        t.setLoggingInfo(loggingInfoList);

        genericResourceService.add(getDraftResourceType(), t);

        return t;
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public T updateDraft(T t, Authentication auth) {
        logger.trace("Attempting to update the Draft Resource: {}", t);
        try {
            genericResourceService.update(getDraftResourceType(), t.getId(), t);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return t;
    }

    @Override
    @CacheEvict(value = CACHE_PROVIDERS, allEntries = true)
    public void deleteDraft(String id, Authentication authentication) {

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
        Resource resource = genericResourceService.searchResource(getDraftResourceType(), t.getId(), true);
        resource.setResourceTypeName(getDraftResourceType());
        resourceService.changeResourceType(resource, providerResourceType);

        return t;
    }

    @Override
    public T transformToDraft(String id, Authentication auth) {
        return transformToDraft(get(id), auth);
    }

    @Override
    public T transformToDraft(T t, Authentication auth) {
        logger.trace("Attempting to transform the Resource with id '{}' to Draft", t.getId());
        if (genericResourceService.get(getDraftResourceType(), t.getId())) {
            throw new ResourceAlreadyExistsException(String.format("Resource with id = '%s' already exists!", t.getId()));
        }

        ResourceType draftResourceType = resourceTypeService.getResourceType(getDraftResourceType());
        Resource resource = genericResourceService.searchResource(getResourceType(), t.getId(), true);
        resource.setResourceTypeName(getDraftResourceType());
        resourceService.changeResourceType(resource, draftResourceType);

        return t;
    }
}
