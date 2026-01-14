/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.manager.aspects.TriggersAspects;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@org.springframework.stereotype.Service
public class ServiceManager extends TestManager<NewServiceBundle> implements ServiceService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceManager.class);

    private final ProviderService providerService;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final EmailService emailService;
    private final VocabularyService vocabularyService;
    private final PublicServiceService publicServiceManager;
    private final MigrationService migrationService;
    private final DatasourceService datasourceService;
    private final PublicDatasourceService publicDatasourceManager;
    private final ProviderResourcesCommonMethods commonMethods;
    private final SynchronizerService<Service> synchronizerService;
    private final Validator serviceValidator;
    private final FacetLabelService facetLabelService;
    private final GenericResourceService genericResourceService;
    private final RelationshipValidator relationshipValidator;

    @Value("${catalogue.id}")
    private String catalogueId;
    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    public ServiceManager(ProviderService providerService,
                          IdCreator idCreator, @Lazy SecurityService securityService,
                          @Lazy EmailService emailService,
                          @Lazy VocabularyService vocabularyService,
                          @Lazy PublicServiceService publicServiceManager,
                          @Lazy MigrationService migrationService,
                          @Lazy DatasourceService datasourceService,
                          @Lazy PublicDatasourceService publicDatasourceManager,
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
        this.emailService = emailService;
        this.vocabularyService = vocabularyService;
        this.publicServiceManager = publicServiceManager;
        this.migrationService = migrationService;
        this.datasourceService = datasourceService;
        this.publicDatasourceManager = publicDatasourceManager;
        this.commonMethods = commonMethods;
        this.synchronizerService = synchronizerService;
        this.serviceValidator = serviceValidator;
        this.facetLabelService = facetLabelService;
        this.genericResourceService = genericResourceService;
        this.relationshipValidator = relationshipValidator;
    }

    @Override
    protected String getResourceTypeName() {
        return "servicetest";
    }

    //region generic
    @Override
    public NewServiceBundle add(NewServiceBundle service, Authentication auth) {
        NewProviderBundle provider = providerService.get((String) service.getService().get("serviceOwner"),
                service.getCatalogueId());
        onboard(service, provider, auth);
        onboardingValidation(service, provider);
        NewServiceBundle ret = genericResourceService.add(getResourceTypeName(), service);
//        synchronizerService.syncAdd(service.getService()); //TODO: remove this?
        return ret;
    }

    private void onboard(NewServiceBundle service, NewProviderBundle provider, Authentication auth) {
        String catalogueId = service.getCatalogueId();
        if (catalogueId == null || catalogueId.isEmpty() || catalogueId.equals(this.catalogueId)) {
            if (provider.getTemplateStatus().equals("approved template")) {
                service.markOnboard(vocabularyService.get("approved").getId(), false, auth, null);
                service.setActive(true);
            } else {
                service.markOnboard(vocabularyService.get("pending").getId(), false, auth, null);
            }
            service.setCatalogueId(this.catalogueId);
            service.setId(idCreator.generate(getResourceTypeName()));
//            commonMethods.createIdentifiers(service, getResourceTypeName(), false); //FIXME
        } else {
            service.markOnboard(vocabularyService.get("approved").getId(), true, auth, null);
            commonMethods.checkCatalogueIdConsistency(service, catalogueId);
            idCreator.validateId(service.getId());
//            commonMethods.createIdentifiers(service, getResourceTypeName(), true); //FIXME
        }
        service.setAuditState(Auditable.NOT_AUDITED);
    }

    private void onboardingValidation(NewServiceBundle service, NewProviderBundle provider) {
//        relationshipValidator.checkRelatedResourceIDsConsistency(service); //FIXME
        //TODO: ModelResponseValidator to validate Vocabulary parent-child relationships
//        VocabularyValidationUtils.validateCategories();
//        VocabularyValidationUtils.validateScientificDomains();
        if (!provider.getStatus().equals("approved")) {
            throw new ResourceException(String.format("The Provider '%s' you provided as a Service Owner " +
                    "is not yet approved", provider.getId()), HttpStatus.CONFLICT);
        }
        if (provider.getTemplateStatus().equals("pending template")) {
            throw new ResourceException(String.format("The Provider with id %s has already registered a Resource " +
                    "Template.", provider.getId()), HttpStatus.CONFLICT);
        }
    }

    @Override
    @Transactional
    @TriggersAspects({"AfterServiceUpdateEmails"})
    public NewServiceBundle update(NewServiceBundle service, String comment, Authentication auth) {
        NewServiceBundle existing = get(service.getId(), service.getCatalogueId());
        // check if there are actual changes in the Service
        if (service.equals(existing)) {
            return service;
        }
        service.markUpdate(auth, comment);
//        relationshipValidator.checkRelatedResourceIDsConsistency(service); //FIXME
        checkAndResetServiceOnboarding(service);

        //TODO: ModelResponseValidator to validate Vocabulary parent-child relationships
//        VocabularyValidationUtils.validateCategories();
//        VocabularyValidationUtils.validateScientificDomains();

        try {
            return genericResourceService.update(getResourceTypeName(), service.getId(), service);
//            synchronizerService.syncUpdate(service.getService()); // TODO: remove this?
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkAndResetServiceOnboarding(NewServiceBundle service) {
        NewProviderBundle provider = providerService.get((String) service.getService().get("serviceOwner"),
                service.getCatalogueId());
        // if Resource's status = "rejected", update to "pending" & Provider templateStatus to "pending template"
        if (service.getStatus().equals(vocabularyService.get("rejected").getId())) {
            if (provider.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())) {
                service.setStatus(vocabularyService.get("pending").getId());
                service.setActive(false);
                provider.setTemplateStatus(vocabularyService.get("pending template").getId());
                providerService.update(provider, "system update", securityService.getAdminAccess()); //TODO: this or generic?
            }
        }
    }

    @Override
    @Transactional
    public void delete(NewServiceBundle bundle) {
        commonMethods.blockResourceDeletion(bundle.getStatus(), bundle.getMetadata().isPublished());
//        commonMethods.deleteResourceInteroperabilityRecords(bundle.getId(), getResourceTypeName()); //FIXME
        logger.info("Deleting Service: {} and all its Resource Interoperability Records", bundle.getId());
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
//        synchronizerService.syncDelete(bundle.getStatus()); //TODO: remove this?
    }

    @Transactional
    public NewServiceBundle setStatus(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        NewServiceBundle existing = get(id);
        existing.markOnboard(status, active, auth, null);

        updateProviderTemplateStatus(existing, status);

        logger.info("Verifying Service: {}", existing);
        try {
            return genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateProviderTemplateStatus(NewServiceBundle service, String status) {
        NewProviderBundle provider = providerService.get((String) service.getService().get("serviceOwner"),
                service.getCatalogueId());
        switch (status) {
            case "pending":
                provider.setTemplateStatus("pending template");
                break;
            case "approved":
                provider.setTemplateStatus("approved template");
                break;
            case "rejected":
                provider.setTemplateStatus("rejected template");
                break;
            default:
                break;
        }
        providerService.update(provider, "system update", securityService.getAdminAccess()); //TODO: this or generic?
    }

    @Override
    public NewServiceBundle setActive(String id, Boolean active, Authentication auth) {
        NewServiceBundle existing = get(id);

        NewProviderBundle provider = providerService.get((String) existing.getService().get("serviceOwner"),
                existing.getCatalogueId());
        if (active && !provider.isActive()) {
            throw new ResourceException("You cannot activate the Service, as its Provider is inactive", HttpStatus.CONFLICT);
        }
        if ((existing.getStatus().equals(vocabularyService.get("pending").getId()) ||
                existing.getStatus().equals(vocabularyService.get("rejected").getId())) && !existing.isActive()) {
            throw new ValidationException("You cannot activate this Service, because it is not yet approved.");
        }

        existing.markActive(active, auth);
        try {
            return genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    //endregion

    //region Service-specific
    @Override
    public Paging<NewServiceBundle> getAllServicesOfAProvider(String providerId, String catalogueId,
                                                              int quantity, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("service_owner", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.setQuantity(quantity);
        ff.addOrderBy("name", "asc");
        return getAll(ff, auth);
    }

    public void sendEmailNotificationToProviderForOutdatedService(String id, Authentication auth) {
        NewServiceBundle service = get(id);
        NewProviderBundle provider = providerService.get((String) service.getService().get("serviceOwner"),
                service.getCatalogueId());
        logger.info("Sending email to Provider '{}' for outdated Services", provider.getId());
//        emailService.sendEmailNotificationsToProviderAdminsWithOutdatedResources(service, provider); //FIXME
    }

    //FIXME
//    public ServiceBundle changeProvider(String resourceId, String newProviderId, String comment, Authentication auth) {
//        ServiceBundle serviceBundle = get(resourceId, catalogueId, false);
//        // check Service's status
//        if (!serviceBundle.getStatus().equals("approved")) {
//            throw new ValidationException(String.format("You cannot move Service with id [%s] to another Provider as it" +
//                    "is not yet Approved", serviceBundle.getId()));
//        }
//        ProviderBundle newProvider = providerService.get(newProviderId, auth);
//        ProviderBundle oldProvider = providerService.get(serviceBundle.getService().getCatalogueId(),
//                serviceBundle.getService().getResourceOrganisation(), auth);
//
//        // check that the 2 Providers co-exist under the same Catalogue
//        if (!oldProvider.getProvider().getCatalogueId().equals(newProvider.getProvider().getCatalogueId())) {
//            throw new ValidationException("You cannot move a Service to a Provider of another Catalogue");
//        }
//
//        // update loggingInfo
//        List<LoggingInfo> loggingInfoList = serviceBundle.getLoggingInfo();
//        LoggingInfo loggingInfo;
//        if (comment == null || comment.isEmpty()) {
//            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.MOVE.getKey(),
//                    LoggingInfo.ActionType.MOVED.getKey());
//        } else {
//            loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.MOVE.getKey(),
//                    LoggingInfo.ActionType.MOVED.getKey(), comment);
//        }
//        loggingInfoList.add(loggingInfo);
//        serviceBundle.setLoggingInfo(loggingInfoList);
//
//        // update latestUpdateInfo
//        serviceBundle.setLatestUpdateInfo(loggingInfo);
//
//        // update metadata
//        Metadata metadata = serviceBundle.getMetadata();
//        metadata.setModifiedAt(String.valueOf(System.currentTimeMillis()));
//        metadata.setModifiedBy(AuthenticationInfo.getFullName(auth));
//        metadata.setTerms(null);
//        serviceBundle.setMetadata(metadata);
//
//        // update ResourceOrganisation
//        serviceBundle.getService().setResourceOrganisation(newProviderId);
//
//        // update ResourceProviders
//        List<String> resourceProviders = serviceBundle.getService().getResourceProviders();
//        if (resourceProviders.contains(oldProvider.getId())) {
//            resourceProviders.remove(oldProvider.getId());
//            resourceProviders.add(newProviderId);
//        }
//
//        // add Resource, delete the old one
//        add(serviceBundle, auth);
//        publicServiceManager.delete(get(resourceId, catalogueId, false)); // FIXME: ProviderManagementAspect's deletePublicDatasource is not triggered
//        delete(get(resourceId, catalogueId, false));
//
//        // update other resources which had the old resource ID on their fields
//        migrationService.updateRelatedToTheIdFieldsOfOtherResourcesOfThePortal(resourceId, resourceId); //TODO: SEE IF IT WORKS AS INTENDED AND REMOVE
//
//        // emails to EPOT, old and new Provider
//        emailService.sendEmailsForMovedResources(oldProvider, newProvider, serviceBundle, auth);
//
//        return serviceBundle;
//    }

    @Override
    public Browsing<NewServiceBundle> getMy(FacetFilter filter, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("draft", false); // A Draft Provider cannot have resources
        List<NewProviderBundle> providers = providerService.getMy(ff, auth).getResults();

        if (providers.isEmpty()) {
            return new Browsing<>();
        }

        filter.setResourceType(getResourceTypeName());
        filter.setQuantity(maxQuantity);
        filter.addFilter("published", false);
        filter.addFilter("service_owner", providers.stream().map(NewProviderBundle::getId).toList());
        ff.addOrderBy("name", "asc");
        return genericResourceService.getResults(ff);
    }

    @Override
    public Map<String, List<LinkedHashMap<String, Object>>> getBy(String field, Authentication auth)
            throws NoSuchFieldException {
        return null;
        //FIXME
//        Field serviceField = null;
//        try {
//            serviceField = Service.class.getDeclaredField(field);
//        } catch (NoSuchFieldException e) {
//            logger.warn("Attempt to find field '{}' in Service failed. Trying in ServiceBundle...", field);
//            serviceField = ServiceBundle.class.getDeclaredField(field);
//        }
//        serviceField.setAccessible(true);
//
//        FacetFilter ff = new FacetFilter();
//        ff.setQuantity(maxQuantity);
//        ff.addFilter("published", false);
//        Browsing<NewServiceBundle> services = getAll(ff, auth);
//
//        final Field f = serviceField;
//        final String undef = "undefined";
//        return services.getResults().stream().collect(Collectors.groupingBy(service -> {
//            try {
//                return f.get(service.getPayload()) != null ? f.get(service.getPayload()).toString() : undef;
//            } catch (IllegalAccessException | IllegalArgumentException e) {
//                logger.warn("Warning", e);
//                try {
//                    return f.get(service) != null ? f.get(service).toString() : undef;
//                } catch (IllegalAccessException e1) {
//                    logger.error("ERROR", e1);
//                }
//                return undef;
//            }
//        }, Collectors.mapping((ServiceBundle service) -> service, toList())));
    }

    //FIXME
    @Override
    public List<NewServiceBundle> getByIds(Authentication auth, String... ids) {
        return null;
//        List<NewServiceBundle> resources;
//        resources = Arrays.stream(ids)
//                .map(id ->
//                {
//                    try {
//                        return get(id, null);
//                    } catch (ServiceException | ResourceNotFoundException e) {
//                        return null;
//                    }
//
//                })
//                .filter(Objects::nonNull)
//                .toList();
//        return resources;
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
            if (resourceBundle.getStatus().equals(vocabularyService.get("pending").getId())) {
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

    @Override
    public Paging<ServiceBundle> getRandomResourcesForAuditing(int quantity, int auditingInterval, Authentication auth) {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setQuantity(maxQuantity);
        facetFilter.addFilter("status", "approved");
        facetFilter.addFilter("published", false);

        Browsing<ServiceBundle> serviceBrowsing = getAll(facetFilter, auth);
        List<ServiceBundle> servicesToBeAudited = new ArrayList<>();

        long todayEpochMillis = System.currentTimeMillis();
        long intervalEpochSeconds = Instant.ofEpochMilli(todayEpochMillis)
                .atZone(ZoneId.systemDefault())
                .minusMonths(auditingInterval)
                .toEpochSecond();

        for (ServiceBundle serviceBundle : serviceBrowsing.getResults()) {
            LoggingInfo auditInfo = serviceBundle.getLatestAuditInfo();
            if (auditInfo == null) {
                // Include services that have never been audited
                servicesToBeAudited.add(serviceBundle);
            } else {
                try {
                    long auditEpochSeconds = Long.parseLong(auditInfo.getDate());
                    if (auditEpochSeconds < intervalEpochSeconds) {
                        // Include services that were last audited before the threshold
                        servicesToBeAudited.add(serviceBundle);
                    }
                } catch (NumberFormatException e) {
                }
            }
        }

        // Shuffle the list randomly
        Collections.shuffle(servicesToBeAudited);

        // Limit the list to the requested quantity
        if (servicesToBeAudited.size() > quantity) {
            servicesToBeAudited = servicesToBeAudited.subList(0, quantity);
        }

        return new Browsing<>(servicesToBeAudited.size(), 0, servicesToBeAudited.size(), servicesToBeAudited,
                serviceBrowsing.getFacets());
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
    //endregion

    //region Drafts
    @Override
    public NewServiceBundle addDraft(NewServiceBundle bundle, Authentication auth) {
        bundle.markDraft(auth, null);
        bundle.setId(idCreator.generate(getResourceTypeName()));
        bundle.setCatalogueId(catalogueId);
//        commonMethods.createIdentifiers(bundle, getResourceTypeName(), false); //FIXME

        NewServiceBundle ret = genericResourceService.add(getResourceTypeName(), bundle, false);
        return ret;
    }

    @Override
    public NewServiceBundle updateDraft(NewServiceBundle bundle, Authentication auth) {
        bundle.markUpdate(auth, null);
        try {
            NewServiceBundle ret = genericResourceService.update(getResourceTypeName(), bundle.getId(), bundle, false);
            return ret;
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteDraft(NewServiceBundle bundle) {
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Override
    public NewServiceBundle finalizeDraft(NewServiceBundle service, Authentication auth) {
        NewProviderBundle provider = providerService.get((String) service.getService().get("serviceOwner"),
                service.getCatalogueId());
        if (provider.getTemplateStatus().equals("approved template")) {
            service.markOnboard(vocabularyService.get("approved").getId(), true, auth, null);
        } else {
            service.markOnboard(vocabularyService.get("pending").getId(), false, auth, null);
        }
        service = update(service, auth);

        return service;
    }
    //endregion
}
