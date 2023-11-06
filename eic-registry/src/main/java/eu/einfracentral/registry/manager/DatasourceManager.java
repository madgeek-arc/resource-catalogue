package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.manager.OpenAIREDatasourceManager;
import eu.einfracentral.registry.service.DatasourceService;
import eu.einfracentral.registry.service.ServiceBundleService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.einfracentral.utils.FacetFilterUtils;
import eu.einfracentral.utils.ObjectUtils;
import eu.einfracentral.utils.ProviderResourcesCommonMethods;
import eu.einfracentral.utils.ResourceValidationUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.SearchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;

import java.util.Comparator;
import java.util.List;

@org.springframework.stereotype.Service
public class DatasourceManager extends ResourceManager<DatasourceBundle> implements DatasourceService {

    private static final Logger logger = LogManager.getLogger(DatasourceManager.class);
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final OpenAIREDatasourceManager openAIREDatasourceManager;
    @Value("${project.catalogue.name}")
    private String catalogueName;

    @Autowired
    public DatasourceManager(ServiceBundleService<ServiceBundle> serviceBundleService,
                             @Lazy SecurityService securityService,
                             @Lazy RegistrationMailService registrationMailService,
                             @Lazy VocabularyService vocabularyService,
                             ProviderResourcesCommonMethods commonMethods,
                             OpenAIREDatasourceManager openAIREDatasourceManager) {
        super(DatasourceBundle.class);
        this.serviceBundleService = serviceBundleService;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.commonMethods = commonMethods;
        this.openAIREDatasourceManager = openAIREDatasourceManager;
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
            checkOpenAIREIDExistance(datasourceBundle);
        }
        datasourceBundle.setId(datasourceBundle.getDatasource().getServiceId());
        logger.trace("User '{}' is attempting to add a new Datasource: {}", auth, datasourceBundle);

        this.validate(datasourceBundle);

        datasourceBundle.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(datasourceBundle, auth);
        datasourceBundle.setLoggingInfo(loggingInfoList);
        differentiateInternalFromExternalCatalogueAddition(datasourceBundle);

        super.add(datasourceBundle, null);
        logger.debug("Adding Datasource for Service: {}", datasourceBundle.getDatasource().getServiceId());

        if (datasourceBundle.getDatasource().getCatalogueId().equals(catalogueName)) {
            registrationMailService.sendEmailsForDatasourceExtension(datasourceBundle, "post");
        }
        return datasourceBundle;
    }

    private DatasourceBundle differentiateInternalFromExternalCatalogueAddition(DatasourceBundle datasourceBundle) {
        if (datasourceBundle.getDatasource().getCatalogueId().equals(catalogueName)) {
            datasourceBundle.setActive(false);
            datasourceBundle.setStatus(vocabularyService.get("pending datasource").getId());
            datasourceBundle.setLatestOnboardingInfo(datasourceBundle.getLoggingInfo().get(0));
        } else {
            datasourceBundle.setActive(true);
            datasourceBundle.setStatus(vocabularyService.get("approved datasource").getId());
            LoggingInfo loggingInfo = commonMethods.createLoggingInfo(securityService.getAdminAccess(), LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.APPROVED.getKey());
            datasourceBundle.getLoggingInfo().add(loggingInfo);
            datasourceBundle.setLatestOnboardingInfo(datasourceBundle.getLoggingInfo().get(1));
        }
        return datasourceBundle;
    }

    @Override
    public DatasourceBundle update(DatasourceBundle datasourceBundle, String comment, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Datasource with id '{}'", auth, datasourceBundle.getId());

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
        logger.trace("User '{}' is attempting to update the Datasource: {}", auth, datasourceBundle);

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

    public DatasourceBundle verifyDatasource(String id, String status, Boolean active, Authentication auth) {
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

    public FacetFilter createFacetFilterForFetchingDatasources(MultiValueMap<String, Object> allRequestParams, String catalogueId) {
        FacetFilter ff = FacetFilterUtils.createMultiFacetFilter(allRequestParams);
        allRequestParams.remove("catalogue_id");
        if (catalogueId != null) {
            if (!catalogueId.equals("all")) {
                ff.addFilter("catalogue_id", catalogueId);
            }
        }
        ff.addFilter("published", false);
        ff.setResourceType("datasource");
        return ff;
    }

    // OpenAIRE
    private void checkOpenAIREIDExistance(DatasourceBundle datasourceBundle) {
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