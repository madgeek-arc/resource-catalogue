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
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.dto.CatalogueValue;
import gr.uoa.di.madgik.resourcecatalogue.dto.MapValues;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.manager.aspects.TriggersAspects;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.WorkflowService;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.OrganisationCascadeLifecycleManager;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@org.springframework.stereotype.Service("organisationManager")
public class OrganisationManager extends ResourceCatalogueGenericManager<OrganisationBundle>
        implements OrganisationService {

    private static final Logger logger = LoggerFactory.getLogger(OrganisationManager.class);

    @Value("${catalogue.id}")
    private String catalogueId;
    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    private final GenericResourceService genericResourceService;
    private final ServiceService serviceService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final OrganisationCascadeLifecycleManager cascadeLifecycleService;

    @Autowired
    @Lazy
    TrainingResourceService trainingResourceService;
    @Autowired
    @Lazy
    InteroperabilityRecordService interoperabilityRecordService;


    public OrganisationManager(GenericResourceService genericResourceService,
                               VocabularyService vocabularyService,
                               @Lazy ServiceService serviceService,
                               IdCreator idCreator,
                               ProviderResourcesCommonMethods commonMethods,
                               SecurityService securityService,
                               OrganisationCascadeLifecycleManager cascadeLifecycleService,
                               WorkflowService workflowService) {
        super(genericResourceService, idCreator, securityService, vocabularyService, workflowService);
        this.genericResourceService = genericResourceService;
        this.serviceService = serviceService;
        this.commonMethods = commonMethods;
        this.cascadeLifecycleService = cascadeLifecycleService;
    }

    @Override
    protected String getResourceTypeName() {
        return "organisation";
    }

    //region generic
    @Override
    public OrganisationBundle add(OrganisationBundle bundle, Authentication auth) {
        return super.add(bundle, auth);
//        emailService.sendEmailsToNewlyAddedProviderAdmins(bundle, null); //FIXME
    }

    @Override
//    @TriggersAspects({"HostingLegalEntityVocabularyUpdate", "AfterProviderUpdateEmails"})
    @TriggersAspects({"HostingLegalEntityVocabularyUpdate"})
    public OrganisationBundle update(OrganisationBundle bundle, String comment, Authentication auth) {
        OrganisationBundle existing = get(bundle.getId(), bundle.getCatalogueId());
        // check if there are actual changes in the Organisation
        if (bundle.equals(existing)) {
            return bundle;
        }
        bundle.markUpdate(UserInfo.of(auth), comment);

        //TODO: ModelResponseValidator to validate Vocabulary parent-child relationships
//        VocabularyValidationUtils.validateScientificDomains();

        return genericResourceService.update(getResourceTypeName(), bundle);
    }

    @Override
    @Transactional // if deleteAllRelatedResources() fails, this should also fail
//    @TriggersAspects({"AfterProviderDeletionEmails"})
    public void delete(OrganisationBundle bundle) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // block Public Organisation deletion
        if (bundle.getMetadata().isPublished()) {
            throw new ValidationException("You cannot directly delete a Public Organisation");
        }

        logger.info("Deleting Organisation: {} and all its Resources", bundle.getId());
        cascadeLifecycleService.deleteAllRelatedResources(bundle, auth);
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Override
    @TriggersAspects({"HostingLegalEntityVocabularyUpdate"})
    public OrganisationBundle verify(String id, String status, Boolean active, Authentication auth) {
        Vocabulary statusVocabulary = vocabularyService.getOrElseThrow(status);
        if (!statusVocabulary.getType().equals("Resource state")) {
            throw new ValidationException(String.format("Vocabulary %s does not consist a Resource State!", status));
        }
        OrganisationBundle existing = get(id);
        existing.markOnboard(status, active, UserInfo.of(auth), null);

        logger.info("Verifying Organisation: {}", existing);
        return genericResourceService.update(getResourceTypeName(), existing);
    }

    @Override
    public OrganisationBundle setActive(String id, Boolean active, Authentication auth) {
        OrganisationBundle existing = get(id);

        if ((existing.getStatus().equals(vocabularyService.get("pending").getId()) ||
                existing.getStatus().equals(vocabularyService.get("rejected").getId())) && !existing.isActive()) {
            throw new ValidationException("You cannot activate this Provider, because it is not yet approved.");
        }

        existing.markActive(active, UserInfo.of(auth));
        return genericResourceService.update(getResourceTypeName(), existing);
    }

    @Override
    public OrganisationBundle setSuspend(String id, String catalogueId, boolean suspend, Authentication auth) {
        OrganisationBundle bundle = get(id, catalogueId);
        //TODO: enable and fix if Catalogues return to their original state
//        commonMethods.suspensionValidation(existing, catalogueId, id, suspend);

        logger.info("Suspending Provider: {} and all its Resources", bundle.getId());
        bundle.markSuspend(suspend, auth);
        cascadeLifecycleService.suspendAllRelatedResources(bundle, auth);

        return genericResourceService.update(getResourceTypeName(), bundle);
    }

    @Override
    public Paging<OrganisationBundle> getMy(FacetFilter ff, Authentication auth) {
        return getMyProviders(ff, auth, getResourceTypeName());
    }
    //endregion

    //region Provider-specific
    @Override
    public boolean hasAdminAcceptedTerms(String id, Authentication auth) {
        OrganisationBundle bundle = get(
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "false")
        );
        String userEmail = AuthenticationInfo.getEmail(auth).toLowerCase();

        List<String> providerAdmins = extractEmails(bundle);
        List<String> acceptedTerms = bundle.getMetadata().getTerms();

        if (acceptedTerms == null || acceptedTerms.isEmpty()) {
            return !providerAdmins.contains(userEmail); // false -> show modal, true -> no modal
        }

        return !providerAdmins.contains(userEmail) || acceptedTerms.contains(userEmail); // Show or not modal
    }

    @Override
    public void adminAcceptedTerms(String id, Authentication auth) {
        OrganisationBundle bundle = get(id);
        String userEmail = AuthenticationInfo.getEmail(auth);

        List<String> existingTerms = bundle.getMetadata().getTerms();
        if (existingTerms == null) {
            existingTerms = new ArrayList<>();
        }

        if (!existingTerms.contains(userEmail)) {
            existingTerms.add(userEmail);
            bundle.getMetadata().setTerms(existingTerms);

            try {
                genericResourceService.update(getResourceTypeName(), bundle);
            } catch (ResourceException | ResourceNotFoundException e) {
                logger.info("Could not update terms for Provider with id: '{}'", id);
            }
        }
    }

    @Override
    public void requestProviderDeletion(String providerId, Authentication auth) {
        OrganisationBundle provider = genericResourceService.get(getResourceTypeName(),
                new SearchService.KeyValue("resource_internal_id", providerId),
                new SearchService.KeyValue("published", "false"));

        List<String> userEmails = extractEmails(provider);
        for (String email : userEmails) {
            if (email.equalsIgnoreCase(AuthenticationInfo.getEmail(auth).toLowerCase())) {
//                emailService.informPortalAdminsForProviderDeletion(provider, User.of(auth)); //FIXME
            }
        }
    }

    @Override
    public String determineHostingLegalEntity(String providerName) {
        List<Vocabulary> hostingLegalEntityList = vocabularyService.getByType(Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY);
        for (Vocabulary hle : hostingLegalEntityList) {
            if (hle.getName().equalsIgnoreCase(providerName)) {
                return hle.getId();
            }
        }
        return null;
    }

    // TODO: get rid of this, replace with resource-specific method
    @Override
    public List<MapValues<CatalogueValue>> getAllResourcesUnderASpecificHLE(String hle, Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("hosting_legal_entity", hle);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        List<MapValues<CatalogueValue>> mapValuesList = new ArrayList<>();
        List<OrganisationBundle> providers = getAll(ff, auth).getResults();
        List<ServiceBundle> services = new ArrayList<>();
        List<TrainingResourceBundle> trainingResources = new ArrayList<>();
        List<InteroperabilityRecordBundle> interoperabilityRecords = new ArrayList<>();
        createMapValuesForHLE(providers, "organisation", mapValuesList);
        for (OrganisationBundle organisationBundle : providers) {
            services.addAll(serviceService.getAllEOSCResourcesOfAProvider(organisationBundle.getId(),
                    createFacetFilter(organisationBundle.getCatalogueId()), auth).getResults());
            trainingResources.addAll(trainingResourceService.getAllEOSCResourcesOfAProvider(organisationBundle.getId(),
                    createFacetFilter(organisationBundle.getCatalogueId()), auth).getResults());
            interoperabilityRecords.addAll(interoperabilityRecordService.getAllEOSCResourcesOfAProvider(organisationBundle.getId(),
                    createFacetFilter(organisationBundle.getCatalogueId()), auth).getResults());
        }
        createMapValuesForHLE(services, "service", mapValuesList);
        createMapValuesForHLE(trainingResources, "training_resource", mapValuesList);
        createMapValuesForHLE(interoperabilityRecords, "interoperability_record", mapValuesList);
        return mapValuesList;
    }

    private FacetFilter createFacetFilter(String catalogueId) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("catalogue_id", catalogueId);
        return ff;
    }

    private void createMapValuesForHLE(List<? extends Bundle> resources, String resourceType,
                                       List<MapValues<CatalogueValue>> mapValuesList) {
        MapValues<CatalogueValue> mapValues = new MapValues<>();
        mapValues.setKey(resourceType);
        List<CatalogueValue> valueList = new ArrayList<>();
        for (Bundle obj : resources) {
            CatalogueValue value = new CatalogueValue();
            switch (resourceType) {
                case "organisation", "service", "training_resource", "interoperability_record":
                    value.setId(obj.getId());
                    value.setName((String) obj.getPayload().get("name"));
                    value.setCatalogue(obj.getCatalogueId());
                    break;
                default:
                    break;
            }
            valueList.add(value);
        }
        mapValues.setValues(valueList);
        mapValuesList.add(mapValues);
    }

    private List<String> extractEmails(OrganisationBundle bundle) {
        List<String> emails = new ArrayList<>();

        Object usersObj = bundle.getOrganisation().get("users");
        if (usersObj instanceof Collection<?>) {
            for (Object obj : (Collection<?>) usersObj) {
                if (obj instanceof User user) {
                    emails.add(user.getEmail().toLowerCase());
                }
            }
        }
        return emails;
    }
    //endregion

    //region Drafts
    @Override
    public OrganisationBundle addDraft(OrganisationBundle bundle, Authentication auth) {
        commonMethods.addAuthenticatedUser(bundle.getOrganisation(), auth);

        return super.addDraft(bundle, auth);
    }
    //endregion
}
