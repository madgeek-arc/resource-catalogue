package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.utils.ResourceValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;

import java.util.Comparator;
import java.util.List;

@org.springframework.stereotype.Service
public class DatasourceManager extends ResourceManager<DatasourceBundle> implements DatasourceService {

    private static final Logger logger = LoggerFactory.getLogger(DatasourceManager.class);
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final OpenAIREDatasourceManager openAIREDatasourceManager;
    private final IdCreator idCreator;

    @Value("${catalogue.id}")
    private String catalogueId;

    public DatasourceManager(ServiceBundleService<ServiceBundle> serviceBundleService,
                             @Lazy SecurityService securityService,
                             @Lazy RegistrationMailService registrationMailService,
                             @Lazy VocabularyService vocabularyService,
                             ProviderResourcesCommonMethods commonMethods,
                             OpenAIREDatasourceManager openAIREDatasourceManager,
                             IdCreator idCreator) {
        super(DatasourceBundle.class);
        this.serviceBundleService = serviceBundleService;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.commonMethods = commonMethods;
        this.openAIREDatasourceManager = openAIREDatasourceManager;
        this.idCreator = idCreator;
    }

    @Override
    public String getResourceType() {
        return "datasource";
    }

    public DatasourceBundle get(String datasourceId) {
        Resource res = where(false, new SearchService.KeyValue("resource_internal_id", datasourceId));
        return res != null ? deserialize(res) : null;
    }

    public DatasourceBundle get(String serviceId, String catalogueId) {
        Resource res = where(false, new SearchService.KeyValue("service_id", serviceId), new SearchService.KeyValue("catalogue_id", catalogueId));
        return res != null ? deserialize(res) : null;
    }

    @Override
    public DatasourceBundle add(DatasourceBundle datasourceBundle, Authentication auth) {

        // if Datasource has ID -> check if it exists in OpenAIRE Datasources list
        if (datasourceBundle.getId() != null && !datasourceBundle.getId().equals("")) {
            checkOpenAIREIDExistence(datasourceBundle);
        }
        datasourceBundle.setId(idCreator.generate(getResourceType()));
        logger.trace("Attempting to add a new Datasource: {}", datasourceBundle);

        this.validate(datasourceBundle);

        datasourceBundle.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(datasourceBundle, auth);
        datasourceBundle.setLoggingInfo(loggingInfoList);
        differentiateInternalFromExternalCatalogueAddition(datasourceBundle);

        super.add(datasourceBundle, null);
        logger.debug("Adding Datasource for Service: {}", datasourceBundle.getDatasource().getServiceId());

        return datasourceBundle;
    }

    private void differentiateInternalFromExternalCatalogueAddition(DatasourceBundle datasourceBundle) {
        if (datasourceBundle.getDatasource().getCatalogueId().equals(catalogueId)) {
            datasourceBundle.setActive(false);
            datasourceBundle.setStatus(vocabularyService.get("pending datasource").getId());
            datasourceBundle.setLatestOnboardingInfo(datasourceBundle.getLoggingInfo().get(0));
            registrationMailService.sendEmailsForDatasourceExtension(datasourceBundle, "post");
        } else {
            datasourceBundle.setActive(true);
            datasourceBundle.setStatus(vocabularyService.get("approved datasource").getId());
            LoggingInfo loggingInfo = commonMethods.createLoggingInfo(securityService.getAdminAccess(),
                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey());
            datasourceBundle.getLoggingInfo().add(loggingInfo);
            datasourceBundle.setLatestOnboardingInfo(datasourceBundle.getLoggingInfo().get(1));
        }
    }

    @Override
    public DatasourceBundle update(DatasourceBundle datasourceBundle, String comment, Authentication auth) {
        logger.trace("Attempting to update the Datasource with id '{}'", datasourceBundle.getId());

        DatasourceBundle ret = ObjectUtils.clone(datasourceBundle);
        Resource existingResource = whereID(ret.getId(), true);
        DatasourceBundle existingDatasource = deserialize(existingResource);
        // check if there are actual changes in the Datasource
        if (ret.getDatasource().equals(existingDatasource.getDatasource())) {
            return ret;
        }

        super.validate(ret);
        ret.setMetadata(Metadata.updateMetadata(ret.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(ret, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey(), comment);
        loggingInfoList.add(loggingInfo);
        ret.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        ret.setLatestUpdateInfo(loggingInfo);
        ret.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        ret.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));
        ret.setActive(existingDatasource.isActive());

        // if status = "rejected datasource", update to "pending datasource"
        if (existingDatasource.getStatus().equals(vocabularyService.get("rejected datasource").getId())) {
            ret.setStatus(vocabularyService.get("pending datasource").getId());
        }

        // block user from updating serviceId
        if (!ret.getDatasource().getServiceId().equals(existingDatasource.getDatasource().getServiceId()) && !securityService.hasRole(auth, "ROLE_ADMIN")) {
            throw new ValidationException("You cannot change the Service Id with which this Datasource is related");
        }

        // block catalogueId updates from Provider Admins
        if (!securityService.hasRole(auth, "ROLE_ADMIN")) {
            if (!existingDatasource.getDatasource().getCatalogueId().equals(ret.getDatasource().getCatalogueId())) {
                throw new ValidationException("You cannot change catalogueId");
            }
        }

        existingResource.setPayload(serialize(ret));
        existingResource.setResourceType(resourceType);

        resourceService.updateResource(existingResource);
        logger.debug("Updating Datasource: {}", ret);

        registrationMailService.sendEmailsForDatasourceExtension(ret, "put");
        return ret;
    }

    public void updateBundle(DatasourceBundle datasourceBundle, Authentication auth) {
        logger.trace("Attempting to update the Datasource: {}", datasourceBundle);

        Resource existing = getResource(datasourceBundle.getId());
        if (existing == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not update Datasource with id '%s' because it does not exist",
                            datasourceBundle.getId()));
        }

        existing.setPayload(serialize(datasourceBundle));
        existing.setResourceType(resourceType);

        resourceService.updateResource(existing);
    }

    @Override
    public DatasourceBundle validate(DatasourceBundle datasourceBundle) {
        String serviceId = datasourceBundle.getDatasource().getServiceId();
        String catalogueId = datasourceBundle.getDatasource().getCatalogueId();

        DatasourceBundle existingDatasource = get(serviceId, catalogueId);
        if (existingDatasource != null) {
            throw new ValidationException(String.format("Service [%s] of the Catalogue [%s] has already a Datasource " +
                    "registered, with id: [%s]", serviceId, catalogueId, existingDatasource.getId()));
        }

        // check if Service exists and if User belongs to Resource's Provider Admins
        ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(serviceId, catalogueId, serviceBundleService, "service");
        return super.validate(datasourceBundle);
    }

    public DatasourceBundle verify(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Datasource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist an Datasource state!", status));
        }
        logger.trace("Verifying Datasource with id: '{}' | status -> '{}' | active -> '{}'", id, status, active);
        DatasourceBundle datasourceBundle = get(id);

        // Verify that Service is Approved before proceeding
        if (!serviceBundleService.get(datasourceBundle.getDatasource().getServiceId(), datasourceBundle.getDatasource().getCatalogueId()).getStatus().equals("approved resource")
                && status.equals("approved datasource")) {
            throw new ValidationException("You cannot approve a Datasource when its Service is in Pending or Rejected state");
        }

        datasourceBundle.setStatus(vocabularyService.get(status).getId());

        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(datasourceBundle, auth);
        LoggingInfo loggingInfo;

        switch (status) {
            case "pending datasource":
                break;
            case "approved datasource":
                datasourceBundle.setActive(active);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.APPROVED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                datasourceBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                datasourceBundle.setLatestOnboardingInfo(loggingInfo);
                break;
            case "rejected datasource":
                datasourceBundle.setActive(false);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.REJECTED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                datasourceBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                datasourceBundle.setLatestOnboardingInfo(loggingInfo);
                break;
            default:
                break;
        }

        logger.info("Verifying Datasource: {}", datasourceBundle);
        return super.update(datasourceBundle, auth);
    }

    @Override
    public void delete(DatasourceBundle datasourceBundle) {
        super.delete(datasourceBundle);
        logger.debug("Deleting Datasource: {}", datasourceBundle);
    }

    @Override
    public Paging<DatasourceBundle> getResourceBundles(String catalogueId, String serviceId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("service_id", serviceId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.setQuantity(maxQuantity);
        return this.getAll(ff, auth);
    }

    // OpenAIRE
    private void checkOpenAIREIDExistence(DatasourceBundle datasourceBundle) {
        Datasource datasource = openAIREDatasourceManager.get(datasourceBundle.getId());
        if (datasource != null) {
            datasourceBundle.setOriginalOpenAIREId(datasourceBundle.getId());
        } else {
            throw new ValidationException(String.format("The ID [%s] you provided does not belong to an OpenAIRE Datasource", datasourceBundle.getId()));
        }
    }

    public boolean isDatasourceRegisteredOnOpenAIRE(String id) {
        DatasourceBundle datasourceBundle = get(id);
        boolean found = false;
        String registerBy;
        if (datasourceBundle != null) {
            String originalOpenAIREId = datasourceBundle.getOriginalOpenAIREId();
            if (originalOpenAIREId != null && !originalOpenAIREId.equals("")) {
                registerBy = openAIREDatasourceManager.getRegisterBy(originalOpenAIREId);
                if (registerBy != null && !registerBy.equals("")) {
                    found = true;
                }
            }
        } else {
            throw new ResourceNotFoundException(String.format("There is no Datasource with ID [%s]", id));
        }
        return found;
    }

//    public DatasourceBundle createPublicResource(DatasourceBundle datasourceBundle, Authentication auth){
//        publicDatasourceManager.add(datasourceBundle, auth);
//        return datasourceBundle;
//    }

}