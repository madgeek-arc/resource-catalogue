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
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.manager.aspects.TriggersAspects;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.Auditable;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.utils.RelationshipValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@org.springframework.stereotype.Service("serviceManager")
public class ServiceManager extends ResourceCatalogueGenericManager<ServiceBundle> implements ServiceService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceManager.class);

    private final ProviderService providerService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final GenericResourceService genericResourceService;
    private final RelationshipValidator relationshipValidator;
    private final ModelService modelService;

    @Value("${catalogue.id}")
    private String catalogueId;
    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    public ServiceManager(ProviderService providerService,
                          IdCreator idCreator,
                          SecurityService securityService,
                          VocabularyService vocabularyService,
                          ProviderResourcesCommonMethods commonMethods,
//                          @Qualifier("serviceValidator") Validator serviceValidator,
                          FacetLabelService facetLabelService,
                          GenericResourceService genericResourceService,
                          @Lazy RelationshipValidator relationshipValidator,
                          ModelService modelService) {
        super(genericResourceService, securityService, vocabularyService);
        this.providerService = providerService; // for providers
        this.commonMethods = commonMethods;
        this.genericResourceService = genericResourceService;
        this.modelService = modelService;
        this.relationshipValidator = relationshipValidator;
    }

    @Override
    protected String getResourceTypeName() {
        return "service";
    }

    //region generic
    @Override
    public ServiceBundle add(ServiceBundle service, Authentication auth) {
        ProviderBundle provider = providerService.get((String) service.getService().get("owner"),
                service.getCatalogueId());
        onboard(service, provider, auth);
        onboardingValidation(service, provider);
        ServiceBundle ret = genericResourceService.add(getResourceTypeName(), service);
        return ret;
    }

    private void onboardingValidation(ServiceBundle service, ProviderBundle provider) {
        relationshipValidator.checkRelatedResourceIDsConsistency(service);
        //TODO: ModelResponseValidator to validate Vocabulary parent-child relationships
//        VocabularyValidationUtils.validateCategories();
//        VocabularyValidationUtils.validateScientificDomains();
        if (!provider.getStatus().equals("approved")) {
            throw new ResourceException(String.format("The Provider '%s' you provided as a Owner " +
                    "is not yet approved", provider.getId()), HttpStatus.CONFLICT);
        }
        if (provider.getTemplateStatus().equals("pending template")) {
            throw new ResourceException(String.format("The Provider with id %s has already registered a Resource " +
                    "Template.", provider.getId()), HttpStatus.CONFLICT);
        }
    }

    @Override
    @Transactional
//    @TriggersAspects({"AfterServiceUpdateEmails"})
    public ServiceBundle update(ServiceBundle service, String comment, Authentication auth) {
        ServiceBundle existing = get(service.getId(), service.getCatalogueId());
        // check if there are actual changes in the Service
        if (service.equals(existing)) {
            return service;
        }
        service.markUpdate(UserInfo.of(auth), comment);
        relationshipValidator.checkRelatedResourceIDsConsistency(service);
        checkAndResetServiceOnboarding(service, auth);

        //TODO: ModelResponseValidator to validate Vocabulary parent-child relationships
//        VocabularyValidationUtils.validateCategories();
//        VocabularyValidationUtils.validateScientificDomains();

        try {
            return genericResourceService.update(getResourceTypeName(), service.getId(), service);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkAndResetServiceOnboarding(ServiceBundle service, Authentication auth) {
        ProviderBundle provider = providerService.get((String) service.getService().get("owner"),
                service.getCatalogueId());
        // if Resource's status = "rejected", update to "pending" & Provider templateStatus to "pending template"
        if (service.getStatus().equals(vocabularyService.get("rejected").getId())) {
            if (provider.getTemplateStatus().equals(vocabularyService.get("rejected template").getId())) {
                service.setStatus(vocabularyService.get("pending").getId());
                service.setActive(false);
                provider.setTemplateStatus(vocabularyService.get("pending template").getId());
                providerService.update(provider, "system update", auth);
            }
        }
    }

    @Override
    @Transactional
    public void delete(ServiceBundle bundle) {
        commonMethods.blockResourceDeletion(bundle.getStatus(), bundle.getMetadata().isPublished());
        commonMethods.deleteResourceInteroperabilityRecords(bundle.getId(), getResourceTypeName());
        logger.info("Deleting Service: {} and all its Resource Interoperability Records", bundle.getId());
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Transactional
    public ServiceBundle setStatus(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        ServiceBundle existing = get(id);
        existing.markOnboard(status, active, UserInfo.of(auth), null);

        updateProviderTemplateStatus(existing, status, auth);

        logger.info("Verifying Service: {}", existing);
        try {
            return genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateProviderTemplateStatus(ServiceBundle service, String status, Authentication auth) {
        ProviderBundle provider = providerService.get((String) service.getService().get("owner"),
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
        providerService.update(provider, "system update", auth);
    }

    @Override
    public ServiceBundle setActive(String id, Boolean active, Authentication auth) {
        ServiceBundle existing = get(id);

        ProviderBundle provider = providerService.get((String) existing.getService().get("owner"),
                existing.getCatalogueId());
        if (active && !provider.isActive()) {
            throw new ResourceException("You cannot activate the Service, as its Provider is inactive", HttpStatus.CONFLICT);
        }
        if ((existing.getStatus().equals(vocabularyService.get("pending").getId()) ||
                existing.getStatus().equals(vocabularyService.get("rejected").getId())) && !existing.isActive()) {
            throw new ValidationException("You cannot activate this Service, because it is not yet approved.");
        }

        existing.markActive(active, UserInfo.of(auth));
        try {
            return genericResourceService.update(getResourceTypeName(), id, existing);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    //endregion

    //region EOSC Resource-specific
    @Override
    public Paging<ServiceBundle> getAllEOSCResourcesOfAProvider(String providerId, String catalogueId,
                                                                int quantity, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("owner", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        ff.setQuantity(quantity);
        ff.addOrderBy("name", "asc");
        return getAll(ff, auth);
    }

    public void sendEmailNotificationToProviderForOutdatedEOSCResource(String id, Authentication auth) {
        ServiceBundle service = get(id);
        ProviderBundle provider = providerService.get((String) service.getService().get("owner"),
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
    public Browsing<ServiceBundle> getMy(FacetFilter filter, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("draft", false); // A Draft Provider cannot have resources
        List<ProviderBundle> providers = providerService.getMy(ff, auth).getResults();

        if (providers.isEmpty()) {
            return new Browsing<>();
        }

        filter.setResourceType(getResourceTypeName());
        filter.setQuantity(maxQuantity);
        filter.addFilter("published", false);
        filter.addFilter("owner", providers.stream().map(ProviderBundle::getId).toList());
        ff.addOrderBy("name", "asc");
        return genericResourceService.getResults(ff);
    }

    //FIXME
    @Override
    public List<ServiceBundle> getByIds(Authentication auth, String... ids) {
        List<ServiceBundle> resources;
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
    public Bundle getTemplate(String providerId, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.addFilter("owner", providerId);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        List<ServiceBundle> allProviderServices = getAll(ff, auth).getResults();
        for (ServiceBundle bundle : allProviderServices) {
            if (bundle.getStatus().equals(vocabularyService.get("pending").getId())) {
                return bundle;
            }
        }
        return null;
    }
    //endregion

    //region Drafts
    @Override
    public ServiceBundle addDraft(ServiceBundle bundle, Authentication auth) {
        bundle.markDraft(auth, null);
        bundle.setCatalogueId(catalogueId);
        this.createIdentifiers(bundle, getResourceTypeName(), false);
        bundle.setId(bundle.getIdentifiers().getOriginalId());

        ServiceBundle ret = genericResourceService.add(getResourceTypeName(), bundle, false);
        return ret;
    }

    @Override
    public ServiceBundle updateDraft(ServiceBundle bundle, Authentication auth) {
        bundle.markUpdate(UserInfo.of(auth), null);
        try {
            ServiceBundle ret = genericResourceService.update(getResourceTypeName(), bundle.getId(), bundle, false);
            return ret;
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteDraft(ServiceBundle bundle) {
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Override
    public ServiceBundle finalizeDraft(ServiceBundle service, Authentication auth) {
        ProviderBundle provider = providerService.get((String) service.getService().get("owner"),
                service.getCatalogueId());
        UserInfo user = UserInfo.of(auth);
        if (provider.getTemplateStatus().equals("approved template")) {
            service.markOnboard(vocabularyService.get("approved").getId(), true, user, null);
        } else {
            service.markOnboard(vocabularyService.get("pending").getId(), false, user, null);
        }
        service = update(service, auth);

        return service;
    }
    //endregion
}
