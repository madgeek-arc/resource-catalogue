/**
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.Validator;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@org.springframework.stereotype.Service
public class ServiceBundleManager extends ResourceCatalogueManager<ServiceBundle> implements ServiceBundleService<ServiceBundle> {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBundleManager.class);

    private final ProviderService providerService;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final VocabularyService vocabularyService;
    private final CatalogueService catalogueService;
    private final PublicServiceService publicServiceManager;
    private final MigrationService migrationService;
    private final DatasourceService datasourceService;
    private final HelpdeskService helpdeskService;
    private final MonitoringService monitoringService;
    private final PublicHelpdeskService publicHelpdeskManager;
    private final PublicMonitoringService publicMonitoringManager;
    private final PublicDatasourceService publicDatasourceManager;
    private final ResourceInteroperabilityRecordService resourceInteroperabilityRecordService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final SynchronizerService<Service> synchronizerService;
    private final Validator serviceValidator;
    private final FacetLabelService facetLabelService;
    private final GenericResourceService genericResourceService;
    private final RelationshipValidator relationshipValidator;

    @Value("${catalogue.id}")
    private String catalogueId;

    public ServiceBundleManager(ProviderService providerService,
                                IdCreator idCreator, @Lazy SecurityService securityService,
                                @Lazy RegistrationMailService registrationMailService,
                                @Lazy VocabularyService vocabularyService,
                                CatalogueService catalogueService,
                                @Lazy PublicServiceService publicServiceManager,
                                @Lazy MigrationService migrationService,
                                @Lazy DatasourceService datasourceService,
                                @Lazy HelpdeskService helpdeskService,
                                @Lazy MonitoringService monitoringService,
                                @Lazy PublicHelpdeskService publicHelpdeskManager,
                                @Lazy PublicMonitoringService publicMonitoringManager,
                                @Lazy PublicDatasourceService publicDatasourceManager,
                                @Lazy ResourceInteroperabilityRecordService
                                        resourceInteroperabilityRecordService,
                                @Lazy ProviderResourcesCommonMethods commonMethods,
                                SynchronizerService<Service> synchronizerService,
                                @Qualifier("serviceValidator") Validator serviceValidator,
                                FacetLabelService facetLabelService,
                                GenericResourceService genericResourceService,
                                @Lazy RelationshipValidator relationshipValidator) {
        super(ServiceBundle.class);
        this.providerService = providerService; // for providers
        this.idCreator = idCreator;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.vocabularyService = vocabularyService;
        this.catalogueService = catalogueService;
        this.publicServiceManager = publicServiceManager;
        this.migrationService = migrationService;
        this.datasourceService = datasourceService;
        this.helpdeskService = helpdeskService;
        this.monitoringService = monitoringService;
        this.publicHelpdeskManager = publicHelpdeskManager;
        this.publicMonitoringManager = publicMonitoringManager;
        this.publicDatasourceManager = publicDatasourceManager;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
        this.commonMethods = commonMethods;
        this.synchronizerService = synchronizerService;
        this.serviceValidator = serviceValidator;
        this.facetLabelService = facetLabelService;
        this.genericResourceService = genericResourceService;
        this.relationshipValidator = relationshipValidator;
    }

    @Override
    public String getResourceTypeName() {
        return "service";
    }

    @Override
    public ServiceBundle addResource(ServiceBundle serviceBundle, Authentication auth) {
        return addResource(serviceBundle, null, auth);
    }

    @Override
    public ServiceBundle addResource(ServiceBundle serviceBundle, String catalogueId, Authentication auth) {
        if (catalogueId == null || catalogueId.isEmpty() || catalogueId.equals(this.catalogueId)) { // add catalogue service
            serviceBundle.getService().setCatalogueId(this.catalogueId);
            serviceBundle.setId(idCreator.generate(getResourceTypeName()));
            commonMethods.createIdentifiers(serviceBundle, getResourceTypeName(), false);
        } else { // add provider from external catalogue
            commonMethods.checkCatalogueIdConsistency(serviceBundle, catalogueId);
            idCreator.validateId(serviceBundle.getId());
            commonMethods.createIdentifiers(serviceBundle, getResourceTypeName(), true);
        }
        relationshipValidator.checkRelatedResourceIDsConsistency(serviceBundle);

        ProviderBundle providerBundle = providerService.get(serviceBundle.getService().getCatalogueId(),
                serviceBundle.getService().getResourceOrganisation(), auth);
        if (providerBundle == null) {
            throw new CatalogueResourceNotFoundException(String.format("Provider with id '%s' and catalogueId '%s' does not exist",
                    serviceBundle.getService().getResourceOrganisation(), serviceBundle.getService().getCatalogueId()));
        }
        // check if Provider is approved
        if (!providerBundle.getStatus().equals("approved provider")) {
            throw new ResourceException(String.format("The Provider '%s' you provided as a Resource Organisation is not yet approved",
                    serviceBundle.getService().getResourceOrganisation()), HttpStatus.CONFLICT);
        }
        // check Provider's templateStatus
        if (providerBundle.getTemplateStatus().equals("pending template")) {
            throw new ResourceException(String.format("The Provider with id %s has already registered a Resource " +
                    "Template.", providerBundle.getId()), HttpStatus.CONFLICT);
        }
        // if Resource version is empty set it null
        if ("".equals(serviceBundle.getService().getVersion())) {
            serviceBundle.getService().setVersion(null);
        }

        validate(serviceBundle);

        boolean active = providerBundle
                .getTemplateStatus()
                .equals("approved template");
        serviceBundle.setActive(active);

        // create new Metadata if not exists
        if (serviceBundle.getMetadata() == null) {
            serviceBundle.setMetadata(Metadata.createMetadata(AuthenticationInfo.getFullName(auth)));
        }

        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(serviceBundle, auth);

        // latestOnboardingInfo
        serviceBundle.setLatestOnboardingInfo(loggingInfoList.getFirst());

        // resource status & extra loggingInfo for Approval
        if (providerBundle.getTemplateStatus().equals("approved template")) {
            serviceBundle.setStatus(vocabularyService.get("approved resource").getId());
            LoggingInfo loggingInfoApproved = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.APPROVED.getKey());
            loggingInfoList.add(loggingInfoApproved);

            // latestOnboardingInfo
            serviceBundle.setLatestOnboardingInfo(loggingInfoApproved);
        } else {
            serviceBundle.setStatus(vocabularyService.get("pending resource").getId());
        }

        // LoggingInfo
        serviceBundle.setLoggingInfo(loggingInfoList);
        serviceBundle.setAuditState(Auditable.NOT_AUDITED);

        logger.info("Adding Service: {}", serviceBundle);
        ServiceBundle ret;

        prettifyServiceTextFields(serviceBundle, ",");

        ret = super.add(serviceBundle, auth);

        synchronizerService.syncAdd(serviceBundle.getPayload());

        return ret;
    }

    @Override
    public ServiceBundle updateResource(ServiceBundle serviceBundle, String comment, Authentication auth) {
        return updateResource(serviceBundle, serviceBundle.getService().getCatalogueId(), comment, auth);
    }

    @Override
    public ServiceBundle updateResource(ServiceBundle serviceBundle, String catalogueId, String comment, Authentication auth) {

        ServiceBundle ret = ObjectUtils.clone(serviceBundle);
        ServiceBundle existingService;
        existingService = get(ret.getService().getId(), ret.getService().getCatalogueId(), false);
        if (ret.getService().equals(existingService.getService())) {
            return ret;
        }

        if (catalogueId == null || catalogueId.isEmpty()) {
            ret.getService().setCatalogueId(this.catalogueId);
        } else {
            commonMethods.checkCatalogueIdConsistency(ret, catalogueId);
        }
        relationshipValidator.checkRelatedResourceIDsConsistency(ret);

        logger.trace("Attempting to update the Service with id '{}' of the Catalogue '{}'",
                ret.getService().getId(), ret.getService().getCatalogueId());
        validate(ret);

        ProviderBundle providerBundle = providerService.get(ret.getService().getCatalogueId(), ret.getService().getResourceOrganisation(), auth);

        // if service version is empty set it null
        if ("".equals(ret.getService().getVersion())) {
            ret.getService().setVersion(null);
        }

        // block Public Service update
        if (existingService.getMetadata().isPublished()) {
            throw new ResourceException("You cannot directly update a Public Service", HttpStatus.FORBIDDEN);
        }

        // update existing service Metadata, ResourceExtras, Identifiers, MigrationStatus
        ret.setMetadata(Metadata.updateMetadata(existingService.getMetadata(), AuthenticationInfo.getFullName(auth)));
        ret.setResourceExtras(existingService.getResourceExtras());
        ret.setIdentifiers(existingService.getIdentifiers());
        ret.setMigrationStatus(existingService.getMigrationStatus());

        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(existingService, auth);
        LoggingInfo loggingInfo;

        // update VS version update
        if (((ret.getService().getVersion() == null) && (existingService.getService().getVersion() == null)) ||
                (ret.getService().getVersion().equals(existingService.getService().getVersion()))) {
            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                    LoggingInfo.ActionType.UPDATED.getKey(), comment);
        } else {
            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                    LoggingInfo.ActionType.UPDATED_VERSION.getKey(), comment);
        }
        loggingInfoList.add(loggingInfo);
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        ret.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        ret.setLatestUpdateInfo(loggingInfo);
        ret.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        ret.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        // set active/status
        ret.setActive(existingService.isActive());
        ret.setStatus(existingService.getStatus());
        ret.setSuspended(existingService.isSuspended());
        ret.setAuditState(commonMethods.determineAuditState(ret.getLoggingInfo()));

        // if Resource's status = "rejected resource", update to "pending resource" & Provider templateStatus to "pending template"
        if (existingService.getStatus().equals(vocabularyService.get("rejected resource").getId())) {
            if (providerBundle.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())) {
                ret.setStatus(vocabularyService.get("pending resource").getId());
                ret.setActive(false);
                providerBundle.setTemplateStatus(vocabularyService.get("pending template").getId());
                providerService.update(providerBundle, null, auth);
            }
        }

        // if a user updates a service with version to a service with null version then while searching for the service
        // you get a "Service already exists" error.
        if (existingService.getService().getVersion() != null && ret.getService().getVersion() == null) {
            throw new ResourceException("You cannot update a Service registered with version to a Service with null version",
                    HttpStatus.CONFLICT);
        }

        // block catalogueId updates from Provider Admins
        if (!securityService.hasRole(auth, "ROLE_ADMIN")) {
            if (!existingService.getService().getCatalogueId().equals(ret.getService().getCatalogueId())) {
                throw new ResourceException("You cannot change catalogueId", HttpStatus.FORBIDDEN);
            }
        }

        prettifyServiceTextFields(serviceBundle, ",");

        ret = super.update(ret, auth);
        logger.info("Updating Service: {}", ret);

        // send notification emails to Portal Admins
        if (ret.getLatestAuditInfo() != null && ret.getLatestUpdateInfo() != null) {
            long latestAudit = Long.parseLong(ret.getLatestAuditInfo().getDate());
            long latestUpdate = Long.parseLong(ret.getLatestUpdateInfo().getDate());
            if (latestAudit < latestUpdate && ret.getLatestAuditInfo().getActionType().equals(LoggingInfo.ActionType.INVALID.getKey())) {
                registrationMailService.notifyPortalAdminsForInvalidServiceUpdate(ret);
            }
        }

        synchronizerService.syncUpdate(serviceBundle.getPayload());

        return ret;
    }

    @Override
    public void delete(ServiceBundle serviceBundle) {
        String catalogue = serviceBundle.getService().getCatalogueId();
        commonMethods.blockResourceDeletion(serviceBundle.getStatus(), serviceBundle.getMetadata().isPublished());
        commonMethods.deleteResourceRelatedServiceSubprofiles(serviceBundle.getId(), catalogue);
        commonMethods.deleteResourceRelatedServiceExtensionsAndResourceInteroperabilityRecords(serviceBundle.getId(), catalogue, "Service");
        logger.info("Deleting Service: {}", serviceBundle);
        super.delete(serviceBundle);
        synchronizerService.syncDelete(serviceBundle.getPayload());
    }

    @Override
    public ServiceBundle validate(ServiceBundle serviceBundle) {
        logger.debug("Validating Service with id: '{}'", serviceBundle.getId());

        super.validate(serviceBundle);
        serviceValidator.validate(serviceBundle, null);
        return serviceBundle;
    }

    public ServiceBundle verify(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        logger.trace("verifyResource with id: '{}' | status: '{}' | active: '{}'", id, status, active);
        ServiceBundle serviceBundle = get(id, catalogueId, false);
        serviceBundle.setStatus(vocabularyService.get(status).getId());
        ProviderBundle resourceProvider = providerService.get(serviceBundle.getService().getCatalogueId(), serviceBundle.getService().getResourceOrganisation(), auth);
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(serviceBundle, auth);
        LoggingInfo loggingInfo;

        switch (status) {
            case "pending resource":
                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("pending template");
                break;
            case "approved resource":
                serviceBundle.setActive(active);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.APPROVED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                serviceBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                serviceBundle.setLatestOnboardingInfo(loggingInfo);

                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("approved template");
                break;
            case "rejected resource":
                serviceBundle.setActive(false);
                loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                        LoggingInfo.ActionType.REJECTED.getKey());
                loggingInfoList.add(loggingInfo);
                loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
                serviceBundle.setLoggingInfo(loggingInfoList);

                // latestOnboardingInfo
                serviceBundle.setLatestOnboardingInfo(loggingInfo);

                // update Provider's templateStatus
                resourceProvider.setTemplateStatus("rejected template");
                break;
            default:
                break;
        }

        logger.info("Verifying Service: {}", serviceBundle);
        try {
            providerService.update(resourceProvider, auth);
        } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
        return super.update(serviceBundle, auth);
    }

    @Override
    public ServiceBundle publish(String serviceId, Boolean active, Authentication auth) {
        ServiceBundle service;
        String activeProvider = "";
        service = this.get(serviceId, catalogueId, false);

        if ((service.getStatus().equals(vocabularyService.get("pending resource").getId()) ||
                service.getStatus().equals(vocabularyService.get("rejected resource").getId())) && !service.isActive()) {
            throw new ValidationException(String.format("You cannot activate this Service, because it's Inactive with status = [%s]", service.getStatus()));
        }

        ProviderBundle providerBundle = providerService.get(service.getService().getCatalogueId(), service.getService().getResourceOrganisation(), auth);
        if (providerBundle.getStatus().equals("approved provider") && providerBundle.isActive()) {
            activeProvider = service.getService().getResourceOrganisation();
        }
        if (active && activeProvider.isEmpty()) {
            throw new ResourceException("Service does not have active Providers", HttpStatus.CONFLICT);
        }
        service.setActive(active);

        List<LoggingInfo> loggingInfoList = commonMethods.createActivationLoggingInfo(service, active, auth);
        loggingInfoList.sort(Comparator.comparing(LoggingInfo::getDate));
        service.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        service.setLatestUpdateInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.UPDATE.getKey()));
        service.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        service.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        // active Service's related resources (ServiceExtensions && Subprofiles)
        publishServiceRelatedResources(service.getId(), service.getService().getCatalogueId(), active, auth);

        this.update(service, auth);
        return service;
    }

    public void publishServiceRelatedResources(String serviceId, String catalogueId, Boolean active, Authentication auth) {
        HelpdeskBundle helpdeskBundle = helpdeskService.get(serviceId, catalogueId);
        MonitoringBundle monitoringBundle = monitoringService.get(serviceId, catalogueId);
        DatasourceBundle datasourceBundle = datasourceService.get(serviceId, catalogueId);
        if (active) {
            logger.info("Activating all related resources of the Service with id: '{}'", serviceId);
        } else {
            logger.info("Deactivating all related resources of the Service with id: '{}'", serviceId);
        }
        if (helpdeskBundle != null) {
            publishServiceExtensionsAndSubprofiles(helpdeskBundle, active, auth);
        }
        if (monitoringBundle != null) {
            publishServiceExtensionsAndSubprofiles(monitoringBundle, active, auth);
        }
        if (datasourceBundle != null && datasourceBundle.getStatus().equals("approved datasource")) {
            publishServiceExtensionsAndSubprofiles(datasourceBundle, active, auth);
        }
    }

    private void publishServiceExtensionsAndSubprofiles(Bundle<?> bundle, boolean active, Authentication auth) {
        List<LoggingInfo> loggingInfoList = commonMethods.createActivationLoggingInfo(bundle, active, auth);

        // update Bundle's fields
        bundle.setLoggingInfo(loggingInfoList);
        bundle.setLatestUpdateInfo(loggingInfoList.getLast());
        bundle.setActive(active);

        if (bundle instanceof HelpdeskBundle) {
            try {
                logger.debug("Setting Helpdesk '{}' of the Service '{}' of the '{}' Catalogue to active: '{}'",
                        bundle.getId(), ((HelpdeskBundle) bundle).getHelpdesk().getServiceId(),
                        ((HelpdeskBundle) bundle).getHelpdesk().getCatalogueId(), bundle.isActive());
                helpdeskService.updateBundle((HelpdeskBundle) bundle, auth);
                HelpdeskBundle publicHelpdeskBundle =
                        publicHelpdeskManager.getOrElseReturnNull(bundle.getIdentifiers().getPid(),
                                ((HelpdeskBundle) bundle).getHelpdesk().getCatalogueId());
                if (publicHelpdeskBundle != null) {
                    publicHelpdeskManager.update((HelpdeskBundle) bundle, auth);
                }
            } catch (ResourceNotFoundException e) {
                logger.error("Could not update Helpdesk '{}' of the Service '{}' of the '{}' Catalogue",
                        bundle.getId(), ((HelpdeskBundle) bundle).getHelpdesk().getServiceId(),
                        ((HelpdeskBundle) bundle).getHelpdesk().getCatalogueId());
            }
        } else if (bundle instanceof MonitoringBundle) {
            try {
                logger.debug("Setting Monitoring '{}' of the Service '{}' of the '{}' Catalogue to active: '{}'",
                        bundle.getId(), ((MonitoringBundle) bundle).getMonitoring().getServiceId(),
                        ((MonitoringBundle) bundle).getMonitoring().getCatalogueId(), bundle.isActive());
                monitoringService.updateBundle((MonitoringBundle) bundle, auth);
                MonitoringBundle publicMonitoringBundle =
                        publicMonitoringManager.getOrElseReturnNull(bundle.getIdentifiers().getPid(),
                                ((MonitoringBundle) bundle).getMonitoring().getCatalogueId());
                if (publicMonitoringBundle != null) {
                    publicMonitoringManager.update((MonitoringBundle) bundle, auth);
                }
            } catch (ResourceNotFoundException e) {
                logger.error("Could not update Monitoring '{}' of the Service '{}' of the '{}' Catalogue",
                        bundle.getId(), ((MonitoringBundle) bundle).getMonitoring().getServiceId(),
                        ((MonitoringBundle) bundle).getMonitoring().getCatalogueId());
            }
        } else {
            try {
                logger.debug("Setting Datasource '{}' of the Service '{}' of the '{}' Catalogue to active: '{}'",
                        bundle.getId(), ((DatasourceBundle) bundle).getDatasource().getServiceId(),
                        ((DatasourceBundle) bundle).getDatasource().getCatalogueId(), bundle.isActive());
                datasourceService.updateBundle((DatasourceBundle) bundle, auth);
                DatasourceBundle publicDatasourceBundle =
                        publicDatasourceManager.getOrElseReturnNull(bundle.getIdentifiers().getPid(), ((DatasourceBundle) bundle).getDatasource().getCatalogueId());
                if (publicDatasourceBundle != null) {
                    publicDatasourceManager.update((DatasourceBundle) bundle, auth);
                }
            } catch (ResourceNotFoundException e) {
                logger.error("Could not update Datasource '{}' of the Service '{}' of the '{}' Catalogue",
                        bundle.getId(), ((DatasourceBundle) bundle).getDatasource().getServiceId(),
                        ((DatasourceBundle) bundle).getDatasource().getCatalogueId());
            }
        }
    }

    @Override
    public ServiceBundle audit(String serviceId, String catalogueId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        ServiceBundle service = get(serviceId, catalogueId, false);
        ProviderBundle provider = providerService.get(service.getService().getCatalogueId(), service.getService().getResourceOrganisation(), auth);
        commonMethods.auditResource(service, comment, actionType, auth);
        if (actionType.getKey().equals(LoggingInfo.ActionType.VALID.getKey())) {
            service.setAuditState(Auditable.VALID);
        }
        if (actionType.getKey().equals(LoggingInfo.ActionType.INVALID.getKey())) {
            service.setAuditState(Auditable.INVALID_AND_NOT_UPDATED);
        }

        // send notification emails to Provider Admins
        registrationMailService.notifyProviderAdminsForBundleAuditing(service, provider.getProvider().getUsers());

        logger.info("User '{}-{}' audited Service '{}'-'{}' with [actionType: {}]",
                AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase(),
                service.getService().getId(), service.getService().getName(), actionType);
        return super.update(service, auth);
    }

    @Override
    public List<ServiceBundle> getResourceBundles(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("name", "asc");
        return this.getAll(ff, auth).getResults();
    }

    @Override
    public Paging<ServiceBundle> getResourceBundles(String catalogueId, String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("name", "asc");
        return this.getAll(ff, auth);
    }

    @Override
    public List<Service> getResources(String providerId, Authentication auth) {
        ProviderBundle providerBundle = providerService.get(providerId, catalogueId, false);
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("name", "asc");
        if (auth != null && auth.isAuthenticated()) {
            User user = User.of(auth);
            // if user is ADMIN/EPOT or Provider Admin on the specific Provider, return its Services
            if (securityService.hasRole(auth, "ROLE_ADMIN") || securityService.hasRole(auth, "ROLE_EPOT") ||
                    securityService.userHasAdminAccess(user, providerId)) {
                return this.getAll(ff, auth).getResults().stream().map(ServiceBundle::getService).collect(Collectors.toList());
            }
        }
        // else return Provider's Services ONLY if he is active
        if (providerBundle.getStatus().equals(vocabularyService.get("approved provider").getId())) {
            return this.getAll(ff, null).getResults().stream().map(ServiceBundle::getService).collect(Collectors.toList());
        }
        throw new InsufficientAuthenticationException("You cannot view the Services of the specific Provider");
    }

    // for sendProviderMails on RegistrationMailService AND StatisticsManager
    public List<Service> getResourcesByProvider(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("name", "asc");
        return this.getAll(ff, securityService.getAdminAccess()).getResults().stream().map(ServiceBundle::getService).collect(Collectors.toList());
    }

    public void sendEmailNotificationsToProvidersWithOutdatedResources(String resourceId, Authentication auth) {
        ServiceBundle serviceBundle = get(resourceId, catalogueId, false);
        ProviderBundle providerBundle = providerService.get(serviceBundle.getService().getResourceOrganisation(), serviceBundle.getService().getCatalogueId(), false);
        logger.info("Mailing provider '{}'-'{}' for outdated Resources", providerBundle.getId(), providerBundle.getProvider().getName());
        registrationMailService.sendEmailNotificationsToProviderAdminsWithOutdatedResources(serviceBundle, providerBundle);
    }

    public ServiceBundle changeProvider(String resourceId, String newProviderId, String comment, Authentication auth) {
        ServiceBundle serviceBundle = get(resourceId, catalogueId, false);
        // check Service's status
        if (!serviceBundle.getStatus().equals("approved resource")) {
            throw new ValidationException(String.format("You cannot move Service with id [%s] to another Provider as it" +
                    "is not yet Approved", serviceBundle.getId()));
        }
        ProviderBundle newProvider = providerService.get(newProviderId, auth);
        ProviderBundle oldProvider = providerService.get(serviceBundle.getService().getCatalogueId(),
                serviceBundle.getService().getResourceOrganisation(), auth);

        // check that the 2 Providers co-exist under the same Catalogue
        if (!oldProvider.getProvider().getCatalogueId().equals(newProvider.getProvider().getCatalogueId())) {
            throw new ValidationException("You cannot move a Service to a Provider of another Catalogue");
        }

        // update loggingInfo
        List<LoggingInfo> loggingInfoList = serviceBundle.getLoggingInfo();
        LoggingInfo loggingInfo;
        if (comment == null || comment.isEmpty()) {
            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.MOVE.getKey(),
                    LoggingInfo.ActionType.MOVED.getKey());
        } else {
            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.MOVE.getKey(),
                    LoggingInfo.ActionType.MOVED.getKey(), comment);
        }
        loggingInfoList.add(loggingInfo);
        serviceBundle.setLoggingInfo(loggingInfoList);

        // update latestUpdateInfo
        serviceBundle.setLatestUpdateInfo(loggingInfo);

        // update metadata
        Metadata metadata = serviceBundle.getMetadata();
        metadata.setModifiedAt(String.valueOf(System.currentTimeMillis()));
        metadata.setModifiedBy(AuthenticationInfo.getFullName(auth));
        metadata.setTerms(null);
        serviceBundle.setMetadata(metadata);

        // update ResourceOrganisation
        serviceBundle.getService().setResourceOrganisation(newProviderId);

        // update ResourceProviders
        List<String> resourceProviders = serviceBundle.getService().getResourceProviders();
        if (resourceProviders.contains(oldProvider.getId())) {
            resourceProviders.remove(oldProvider.getId());
            resourceProviders.add(newProviderId);
        }

        // add Resource, delete the old one
        add(serviceBundle, auth);
        publicServiceManager.delete(get(resourceId, catalogueId, false)); // FIXME: ProviderManagementAspect's deletePublicDatasource is not triggered
        delete(get(resourceId, catalogueId, false));

        // update other resources which had the old resource ID on their fields
        migrationService.updateRelatedToTheIdFieldsOfOtherResourcesOfThePortal(resourceId, resourceId); //TODO: SEE IF IT WORKS AS INTENDED AND REMOVE

        // emails to EPOT, old and new Provider
        registrationMailService.sendEmailsForMovedResources(oldProvider, newProvider, serviceBundle, auth);

        return serviceBundle;
    }

    public ServiceBundle createPublicResource(ServiceBundle serviceBundle, Authentication auth) {
        publicServiceManager.add(serviceBundle, auth);
        return serviceBundle;
    }

    @Override
    public ServiceBundle suspend(String serviceId, String catalogueId, boolean suspend, Authentication auth) {
        ServiceBundle serviceBundle = get(serviceId, catalogueId, false);
        commonMethods.suspensionValidation(serviceBundle, serviceBundle.getService().getCatalogueId(),
                serviceBundle.getService().getResourceOrganisation(), suspend, auth);
        commonMethods.suspendResource(serviceBundle, suspend, auth);
        // suspend Service's sub-profiles
        DatasourceBundle datasourceBundle = datasourceService.get(serviceId, catalogueId);
        if (datasourceBundle != null) {
            try {
                commonMethods.suspendResource(datasourceBundle, suspend, auth);
                datasourceService.update(datasourceBundle, auth);
            } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        // suspend Service's extensions
        HelpdeskBundle helpdeskBundle = helpdeskService.get(serviceId, serviceBundle.getService().getCatalogueId());
        if (helpdeskBundle != null) {
            try {
                commonMethods.suspendResource(helpdeskBundle, suspend, auth);
                helpdeskService.update(helpdeskBundle, auth);
            } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        MonitoringBundle monitoringBundle = monitoringService.get(serviceId, serviceBundle.getService().getCatalogueId());
        if (monitoringBundle != null) {
            try {
                commonMethods.suspendResource(monitoringBundle, suspend, auth);
                monitoringService.update(monitoringBundle, auth);
            } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        // suspend ResourceInteroperabilityRecord
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle = resourceInteroperabilityRecordService.getWithResourceId(serviceId);
        if (resourceInteroperabilityRecordBundle != null) {
            try {
                commonMethods.suspendResource(resourceInteroperabilityRecordBundle, suspend, auth);
                resourceInteroperabilityRecordService.update(resourceInteroperabilityRecordBundle, auth);
            } catch (gr.uoa.di.madgik.registry.exception.ResourceNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return super.update(serviceBundle, auth);
    }

    @Override
    public Browsing<ServiceBundle> getAll(FacetFilter filter, Authentication auth) {
        updateFacetFilterConsideringTheAuthorization(filter, auth);
        filter.setBrowseBy(this.getBrowseBy());
        filter.setResourceType(getResourceTypeName());

        Browsing<ServiceBundle> resources;
        resources = genericResourceService.getResults(filter);
        if (!resources.getResults().isEmpty() && !resources.getFacets().isEmpty()) {
            resources.setFacets(facetLabelService.generateLabels(resources.getFacets()));
        }

        return resources;
    }

    @Override
    public Browsing<ServiceBundle> getMy(FacetFilter filter, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<ProviderBundle> providers = providerService.getMy(ff, auth).getResults();
        if (providers.isEmpty()) {
            return new Browsing<>();
        }

        if (filter == null) {
            filter = new FacetFilter();
            filter.setQuantity(maxQuantity);
        }
        filter.addFilter("resource_organisation", providers.stream().map(ProviderBundle::getId).toList());
        filter.setResourceType(getResourceTypeName());
        return this.getAll(filter, auth);
    }

    @Override
    public Map<String, List<ServiceBundle>> getBy(String field, Authentication auth) throws NoSuchFieldException {
        Field serviceField = null;
        try {
            serviceField = Service.class.getDeclaredField(field);
        } catch (NoSuchFieldException e) {
            logger.warn("Attempt to find field '{}' in Service failed. Trying in ServiceBundle...", field);
            serviceField = ServiceBundle.class.getDeclaredField(field);
        }
        serviceField.setAccessible(true);

        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("published", false);
        Browsing<ServiceBundle> services = getAll(ff, auth);

        final Field f = serviceField;
        final String undef = "undefined";
        return services.getResults().stream().collect(Collectors.groupingBy(service -> {
            try {
                return f.get(service.getPayload()) != null ? f.get(service.getPayload()).toString() : undef;
            } catch (IllegalAccessException | IllegalArgumentException e) {
                logger.warn("Warning", e);
                try {
                    return f.get(service) != null ? f.get(service).toString() : undef;
                } catch (IllegalAccessException e1) {
                    logger.error("ERROR", e1);
                }
                return undef;
            }
        }, Collectors.mapping((ServiceBundle service) -> service, toList())));
    }

    @Override
    public List<ServiceBundle> getByIds(Authentication auth, String... ids) {
        List<ServiceBundle> resources;
        resources = Arrays.stream(ids)
                .map(id ->
                {
                    try {
                        return get(id, null, false);
                    } catch (ServiceException | ResourceNotFoundException e) {
                        return null;
                    }

                })
                .filter(Objects::nonNull)
                .toList();
        return resources;
    }

    @Override
    public boolean exists(SearchService.KeyValue... ids) {
        Resource resource;
        resource = this.searchService.searchFields(getResourceTypeName(), ids);
        return resource != null;
    }

    @Override
    public Bundle<?> getResourceTemplate(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        List<ServiceBundle> allProviderResources = getAll(ff, auth).getResults();
        for (ServiceBundle resourceBundle : allProviderResources) {
            if (resourceBundle.getStatus().equals(vocabularyService.get("pending resource").getId())) {
                return resourceBundle;
            }
        }
        return null;
    }

    @Override
    protected Browsing<ServiceBundle> getResults(FacetFilter filter) {
        Browsing<ServiceBundle> browsing;
        filter.setResourceType(getResourceTypeName());
        browsing = super.getResults(filter);

        browsing.setFacets(createCorrectFacets(browsing.getFacets(), filter));
        return browsing;
    }

    public List<Facet> createCorrectFacets(List<Facet> serviceFacets, FacetFilter ff) {
        ff.setQuantity(0);

        Map<String, List<Object>> allFilters = ff.getFilterLists();

        List<String> reverseOrderedKeys = new LinkedList<>(allFilters.keySet());
        Collections.reverse(reverseOrderedKeys);

        for (String filterKey : reverseOrderedKeys) {
            Map<String, List<Object>> someFilters = new LinkedHashMap<>(allFilters);

            // if last filter is "active" continue to next iteration
            if ("active".equals(filterKey)) {
                continue;
            }
            someFilters.remove(filterKey);

            FacetFilter facetFilter = FacetFilter.from(someFilters);
            facetFilter.setResourceType(getResourceTypeName());
            facetFilter.setBrowseBy(Collections.singletonList(filterKey));
            List<Facet> facetsCategory = getResults(facetFilter).getFacets(); // CORRECT FACETS ?

            for (Facet facet : serviceFacets) {
                if (facet.getField().equals(filterKey)) {
                    for (Facet facetCategory : facetsCategory) {
                        if (facetCategory.getField().equals(facet.getField())) {
                            serviceFacets.set(serviceFacets.indexOf(facet), facetCategory);
                            break;
                        }
                    }
                    break;
                }
            }
            break;
        }

        return removeEmptyFacets(serviceFacets);
    }

    private List<Facet> removeEmptyFacets(List<Facet> facetList) {
        return facetList.stream().filter(facet -> !facet.getValues().isEmpty()).toList();
    }

    // FIXME: not working...
    @Override
    public Paging<ServiceBundle> getRandomResources(FacetFilter ff, String auditingInterval, Authentication auth) {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setQuantity(maxQuantity);
        facetFilter.addFilter("status", "approved resource");
        facetFilter.addFilter("published", false);
        Browsing<ServiceBundle> serviceBrowsing = getAll(facetFilter, auth);
        List<ServiceBundle> servicesToBeAudited = new ArrayList<>();
        long todayEpochTime = System.currentTimeMillis();
        long interval = Instant.ofEpochMilli(todayEpochTime).atZone(ZoneId.systemDefault()).minusMonths(Integer.parseInt(auditingInterval)).toEpochSecond();
        for (ServiceBundle serviceBundle : serviceBrowsing.getResults()) {
            if (serviceBundle.getLatestAuditInfo() != null) {
                if (Long.parseLong(serviceBundle.getLatestAuditInfo().getDate()) > interval) {
                    servicesToBeAudited.add(serviceBundle);
                }
            }
        }
        Collections.shuffle(servicesToBeAudited);
        if (servicesToBeAudited.size() > ff.getQuantity()) {
            servicesToBeAudited.subList(ff.getQuantity(), servicesToBeAudited.size()).clear();
        }
        return new Browsing<>(servicesToBeAudited.size(), 0, servicesToBeAudited.size(), servicesToBeAudited, serviceBrowsing.getFacets());
    }

    @Override
    public ServiceBundle updateEOSCIFGuidelines(String resourceId, String catalogueId, List<EOSCIFGuidelines> eoscIFGuidelines, Authentication auth) {
        ServiceBundle bundle = get(resourceId, catalogueId, false);
        ResourceExtras resourceExtras = bundle.getResourceExtras();
        if (resourceExtras == null) {
            ResourceExtras newResourceExtras = new ResourceExtras();
            List<EOSCIFGuidelines> newEOSCIFGuidelines = new ArrayList<>(eoscIFGuidelines);
            newResourceExtras.setEoscIFGuidelines(newEOSCIFGuidelines);
            bundle.setResourceExtras(newResourceExtras);
        } else {
            bundle.getResourceExtras().setEoscIFGuidelines(eoscIFGuidelines);
        }
        // check PID consistency
        checkEOSCIFGuidelinesPIDConsistency(bundle);

        createLoggingInfoEntriesForResourceExtraUpdates(bundle, auth);
        validate(bundle);
        update(bundle, auth);
        logger.info("User '{}'-'{}' updated field eoscIFGuidelines of the Resource '{}'",
                AuthenticationInfo.getFullName(auth), AuthenticationInfo.getEmail(auth).toLowerCase(), resourceId);
        return bundle;
    }

    /**
     * Adds spaces after ',' if they don't already exist and removes spaces before
     *
     * @param serviceBundle
     * @param specialCharacters
     * @return
     */
    protected ServiceBundle prettifyServiceTextFields(ServiceBundle serviceBundle, String specialCharacters) {
        serviceBundle.getService().setTagline(TextUtils.prettifyText(serviceBundle.getService().getTagline(), specialCharacters));
        return serviceBundle;
    }

    private void checkEOSCIFGuidelinesPIDConsistency(ServiceBundle serviceBundle) {
        List<String> pidList = new ArrayList<>();
        for (EOSCIFGuidelines eoscIFGuideline : serviceBundle.getResourceExtras().getEoscIFGuidelines()) {
            pidList.add(eoscIFGuideline.getPid());
        }
        Set<String> pidSet = new HashSet<>(pidList);
        if (pidSet.size() < pidList.size()) {
            throw new ValidationException("EOSCIFGuidelines cannot have duplicate PIDs.");
        }
    }

    public ServiceBundle getOrElseReturnNull(String id) {
        ServiceBundle serviceBundle;
        try {
            serviceBundle = get(id);
        } catch (ResourceException | ResourceNotFoundException e) {
            return null;
        }
        return serviceBundle;
    }

    @Override
    public List<ServiceBundle> getInactiveResources(String providerId) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("resource_organisation", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.addFilter("active", false);
        ff.setFrom(0);
        ff.setQuantity(maxQuantity);
        ff.addOrderBy("name", "asc");
        return this.getAll(ff, null).getResults();
    }

    private void createLoggingInfoEntriesForResourceExtraUpdates(ServiceBundle bundle, Authentication auth) {
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(bundle, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey());
        loggingInfoList.add(loggingInfo);
        bundle.setLoggingInfo(loggingInfoList);
    }

    private void updateFacetFilterConsideringTheAuthorization(FacetFilter filter, Authentication auth) {
        // if user is Unauthorized, return active ONLY
        if (auth == null || !auth.isAuthenticated() || (
                !securityService.hasRole(auth, "ROLE_PROVIDER") &&
                !securityService.hasRole(auth, "ROLE_EPOT") &&
                !securityService.hasRole(auth, "ROLE_ADMIN"))) {
            filter.addFilter("active", true);
            filter.addFilter("published", false);
        }
    }
}
