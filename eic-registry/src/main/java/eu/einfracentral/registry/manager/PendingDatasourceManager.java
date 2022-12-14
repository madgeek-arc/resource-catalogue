package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.PendingResourceService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.service.IdCreator;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static eu.einfracentral.config.CacheConfig.*;

@Service("pendingDatasourceManager")
public class PendingDatasourceManager extends ResourceManager<DatasourceBundle> implements PendingResourceService<DatasourceBundle> {

    private static final Logger logger = LogManager.getLogger(PendingDatasourceManager.class);

    private final ResourceBundleService<DatasourceBundle> resourceBundleService;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final VocabularyService vocabularyService;
    private final ProviderManager providerManager;
    private DatasourceBundleManager datasourceBundleManager;

    @Value("${project.catalogue.name}")
    private String catalogueName;

    @Autowired
    public PendingDatasourceManager(ResourceBundleService<DatasourceBundle> resourceBundleService,
                                    IdCreator idCreator, @Lazy SecurityService securityService, @Lazy VocabularyService vocabularyService,
                                    @Lazy ProviderManager providerManager, @Lazy DatasourceBundleManager datasourceBundleManager) {
        super(DatasourceBundle.class);
        this.resourceBundleService = resourceBundleService;
        this.idCreator = idCreator;
        this.securityService = securityService;
        this.vocabularyService = vocabularyService;
        this.providerManager = providerManager;
        this.datasourceBundleManager = datasourceBundleManager;
    }

    @Override
    public String getResourceType() {
        return "pending_datasource";
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public DatasourceBundle add(DatasourceBundle datasourceBundle, Authentication auth) {

        try {
            datasourceBundle.setId(idCreator.createDatasourceId(datasourceBundle));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        // Check if there is a Resource with the specific id
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<DatasourceBundle> resourceList = resourceBundleService.getAll(ff, auth).getResults();
        for (DatasourceBundle existingResource : resourceList){
            if (datasourceBundle.getDatasource().getId().equals(existingResource.getDatasource().getId()) && existingResource.getDatasource().getCatalogueId().equals(catalogueName)) {
                throw new ValidationException(String.format("Datasource with the specific id already exists on the [%s] Catalogue. Please refactor your 'name' field.", catalogueName));
            }
        }
        logger.trace("User '{}' is attempting to add a new Pending Datasource with id {}", auth, datasourceBundle.getId());

        if (datasourceBundle.getMetadata() == null) {
            datasourceBundle.setMetadata(Metadata.createMetadata(User.of(auth).getFullName()));
        }
        if (datasourceBundle.getLoggingInfo() == null){
            LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.DRAFT.getKey(), LoggingInfo.ActionType.CREATED.getKey());
            List<LoggingInfo> loggingInfoList = new ArrayList<>();
            loggingInfoList.add(loggingInfo);
            datasourceBundle.setLoggingInfo(loggingInfoList);
        }

        datasourceBundle.getDatasource().setCatalogueId(catalogueName);
        datasourceBundle.setActive(false);

        super.add(datasourceBundle, auth);

        return datasourceBundle;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public DatasourceBundle update(DatasourceBundle datasourceBundle, Authentication auth) {
        // get existing resource
        Resource existing = this.getPendingResourceViaServiceId(datasourceBundle.getDatasource().getId());
        // block catalogueId updates from Provider Admins
        datasourceBundle.getDatasource().setCatalogueId(catalogueName);
        logger.trace("User '{}' is attempting to update the Pending Datasource with id {}", auth, datasourceBundle.getId());
        datasourceBundle.setMetadata(Metadata.updateMetadata(datasourceBundle.getMetadata(), User.of(auth).getFullName()));
        // save existing resource with new payload
        existing.setPayload(serialize(datasourceBundle));
        existing.setResourceType(resourceType);
        resourceService.updateResource(existing);
        logger.debug("Updating Pending Datasource: {}", datasourceBundle);
        return datasourceBundle;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public DatasourceBundle transformToPending(DatasourceBundle datasourceBundle, Authentication auth) {
        return transformToPending(datasourceBundle.getId(), auth);
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public DatasourceBundle transformToPending(String datasourceId, Authentication auth) {
        logger.trace("User '{}' is attempting to transform the Active Datasource with id {} to Pending", auth, datasourceId);
        DatasourceBundle datasourceBundle = resourceBundleService.get(datasourceId);
        Resource resource = resourceBundleService.getResource(datasourceBundle.getDatasource().getId(), catalogueName);
        resource.setResourceTypeName("datasource");
        resourceService.changeResourceType(resource, resourceType);
        return datasourceBundle;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public DatasourceBundle transformToActive(DatasourceBundle datasourceBundle, Authentication auth) {
        logger.trace("User '{}' is attempting to transform the Pending Datasource with id {} to Active", auth, datasourceBundle.getId());
        resourceBundleService.validate(datasourceBundle);

        // update loggingInfo
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
        List<LoggingInfo> loggingInfoList  = new ArrayList<>();
        if (datasourceBundle.getLoggingInfo() != null) {
            loggingInfoList = datasourceBundle.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
        } else {
            loggingInfoList.add(loggingInfo);
        }
        datasourceBundle.setLoggingInfo(loggingInfoList);

        // latestOnboardInfo
        datasourceBundle.setLatestOnboardingInfo(loggingInfo);

        // set resource status according to Provider's templateStatus
        if (providerManager.get(datasourceBundle.getDatasource().getResourceOrganisation()).getTemplateStatus().equals("approved template")){
            datasourceBundle.setStatus(vocabularyService.get("approved resource").getId());
            LoggingInfo loggingInfoApproved = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
            loggingInfoList.add(loggingInfoApproved);
            datasourceBundle.setActive(true);

            // latestOnboardingInfo
            datasourceBundle.setLatestOnboardingInfo(loggingInfoApproved);
        } else{
            datasourceBundle.setStatus(vocabularyService.get("pending resource").getId());
        }

        datasourceBundle.setMetadata(Metadata.updateMetadata(datasourceBundle.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));

        datasourceBundle = this.update(datasourceBundle, auth);
        ResourceType infraResourceType = resourceTypeService.getResourceType("datasource");
        Resource resource = this.getPendingResourceViaServiceId(datasourceBundle.getDatasource().getId());
        resource.setResourceType(resourceType);
        resourceService.changeResourceType(resource, infraResourceType);

        try {
            datasourceBundle = resourceBundleService.update(datasourceBundle, auth);
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
        }

        return datasourceBundle;
    }

    @Override
    @CacheEvict(cacheNames = {CACHE_VISITS, CACHE_PROVIDERS, CACHE_FEATURED}, allEntries = true)
    public DatasourceBundle transformToActive(String datasourceId, Authentication auth) {
        DatasourceBundle datasourceBundle = this.get(datasourceId);
        return transformToActive(datasourceBundle, auth);
    }

    public Object getPendingRich(String id, Authentication auth) {
        return resourceBundleService.createRichResource(get(id), auth);
    }

    public List<DatasourceBundle> getMy(Authentication auth) {
        List<DatasourceBundle> re = new ArrayList<>();
        return re;
    }

    public boolean hasAdminAcceptedTerms(String providerId, Authentication auth){
        return true;
    }

    public void adminAcceptedTerms(String providerId, Authentication auth){
        // We need this method on PendingProviderManager. Both PendingManagers share the same Service - PendingResourceService
    }

    public Resource getPendingResourceViaServiceId(String datasourceId) {
        Paging<Resource> resources;
        resources = searchService
                .cqlQuery(String.format("pending_datasource_id = \"%s\" AND catalogue_id = \"%s\"", datasourceId, catalogueName),
                        resourceType.getName(), maxQuantity, 0, "modifiedAt", "DESC");
        if (resources.getTotal() > 0) {
            return resources.getResults().get(0);
        }
        return null;
    }

    public Resource getPendingResourceViaProviderId(String providerId) {
        return null;
    }

    public DatasourceBundle getOpenAIREDatasource(Datasource datasource){
        DatasourceBundle datasourceBundle = new DatasourceBundle(datasource);
        // if Datasource has ID -> check if it exists in OpenAIRE Datasources list
        if (datasourceBundle.getId() != null && !datasourceBundle.getId().equals("")){
            datasourceBundleManager.checkOpenAIREIDExistance(datasourceBundle);
        }
        return datasourceBundle;
    }
}
