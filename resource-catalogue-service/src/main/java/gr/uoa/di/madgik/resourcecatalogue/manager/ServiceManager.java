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
import gr.uoa.di.madgik.catalogue.service.ModelService;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ServiceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.manager.aspects.TriggersAspects;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.utils.RelationshipValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

@org.springframework.stereotype.Service
public class ServiceManager extends TestManager<NewServiceBundle> implements ServiceService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceManager.class);

    private final ProviderService providerService;
    private final IdCreator idCreator;
    private final SecurityService securityService;
    private final VocabularyService vocabularyService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final GenericResourceService genericResourceService;
    private final ModelService modelService;

    @Value("${catalogue.id}")
    private String catalogueId;
    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    public ServiceManager(ProviderService providerService,
                          IdCreator idCreator, @Lazy SecurityService securityService,
                          @Lazy EmailService emailService,
                          @Lazy VocabularyService vocabularyService,
                          @Lazy DatasourceService datasourceService,
                          @Lazy ProviderResourcesCommonMethods commonMethods,
                          SynchronizerService<Service> synchronizerService,
                          @Qualifier("serviceValidator") Validator serviceValidator,
                          FacetLabelService facetLabelService,
                          GenericResourceService genericResourceService,
                          @Lazy RelationshipValidator relationshipValidator, ModelService modelService) {
        super(genericResourceService, securityService);
        this.providerService = providerService; // for providers
        this.idCreator = idCreator;
        this.securityService = securityService;
        this.vocabularyService = vocabularyService;
        this.commonMethods = commonMethods;
        this.genericResourceService = genericResourceService;
        this.modelService = modelService;
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

        List<String> fields = modelService.getAllModelFieldNames("m-b-service"); //TODO: I don't like this
        if (!fields.contains(field)) {
            throw new NoSuchFieldException("Unknown field: " + field);
        }

        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("published", false);
        Browsing<NewServiceBundle> serviceBundles = getAll(ff, auth);

        Map<String, List<LinkedHashMap<String, Object>>> result = new LinkedHashMap<>();
        for (NewServiceBundle bundle : serviceBundles.getResults()) {
            LinkedHashMap<String, Object> service = bundle.getService();
            Object keyValue = service.get(field);
            String key = keyValue == null ? "UNKNOWN" : keyValue.toString();
            result.computeIfAbsent(key, k -> new ArrayList<>()).add(service);
        }
        return result;
    }

    //FIXME
    @Override
    public List<NewServiceBundle> getByIds(Authentication auth, String... ids) {
        List<NewServiceBundle> resources;
        resources = Arrays.stream(ids)
                .map(id ->
                {
                    try {
                        return get(id, catalogueId);
                    } catch (ServiceException | ResourceNotFoundException e) {
                        return null;
                    }

                })
                .filter(Objects::nonNull)
                .toList();
        return resources;
    }

    @Override
    public NewBundle getServiceTemplate(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("service_owner", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        List<NewServiceBundle> allProviderServices = getAll(ff, auth).getResults();
        for (NewServiceBundle bundle : allProviderServices) {
            if (bundle.getStatus().equals(vocabularyService.get("pending").getId())) {
                return bundle;
            }
        }
        return null;
    }

    //TODO: find usages or delete
//    @Override
//    protected Browsing<ServiceBundle> getResults(FacetFilter filter) {
//        Browsing<ServiceBundle> browsing;
//        filter.setResourceType(getResourceTypeName());
//        browsing = super.getResults(filter);
//
//        browsing.setFacets(createCorrectFacets(browsing.getFacets(), filter));
//        return browsing;
//    }
//
    //TODO: find usages or delete
//    public List<Facet> createCorrectFacets(List<Facet> serviceFacets, FacetFilter ff) {
//        ff.setQuantity(0);
//
//        Map<String, List<Object>> allFilters = ff.getFilterLists();
//
//        List<String> reverseOrderedKeys = new LinkedList<>(allFilters.keySet());
//        Collections.reverse(reverseOrderedKeys);
//
//        for (String filterKey : reverseOrderedKeys) {
//            Map<String, List<Object>> someFilters = new LinkedHashMap<>(allFilters);
//
//            // if last filter is "active" continue to next iteration
//            if ("active".equals(filterKey)) {
//                continue;
//            }
//            someFilters.remove(filterKey);
//
//            FacetFilter facetFilter = FacetFilter.from(someFilters);
//            facetFilter.setResourceType(getResourceTypeName());
//            facetFilter.setBrowseBy(Collections.singletonList(filterKey));
//            List<Facet> facetsCategory = getResults(facetFilter).getFacets(); // CORRECT FACETS ?
//
//            for (Facet facet : serviceFacets) {
//                if (facet.getField().equals(filterKey)) {
//                    for (Facet facetCategory : facetsCategory) {
//                        if (facetCategory.getField().equals(facet.getField())) {
//                            serviceFacets.set(serviceFacets.indexOf(facet), facetCategory);
//                            break;
//                        }
//                    }
//                    break;
//                }
//            }
//            break;
//        }
//
//        return removeEmptyFacets(serviceFacets);
//    }
//
//    private List<Facet> removeEmptyFacets(List<Facet> facetList) {
//        return facetList.stream().filter(facet -> !facet.getValues().isEmpty()).toList();
//    }


//    private void updateFacetFilterConsideringTheAuthorization(FacetFilter filter, Authentication auth) {
//        // if user is Unauthorized, return active ONLY
//        if (auth == null || !auth.isAuthenticated() || (
//                !securityService.hasRole(auth, "ROLE_PROVIDER") &&
//                        !securityService.hasRole(auth, "ROLE_EPOT") &&
//                        !securityService.hasRole(auth, "ROLE_ADMIN"))) {
//            filter.addFilter("active", true);
//            filter.addFilter("published", false);
//        }
//    }
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
