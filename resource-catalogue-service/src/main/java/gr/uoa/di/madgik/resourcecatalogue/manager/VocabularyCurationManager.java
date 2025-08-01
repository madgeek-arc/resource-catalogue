/*
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
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceAlreadyExistsException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
public class VocabularyCurationManager extends ResourceManager<VocabularyCuration> implements VocabularyCurationService {

    private static final Logger logger = LoggerFactory.getLogger(VocabularyCurationManager.class);
    private final RegistrationMailService registrationMailService;
    private final ProviderService providerService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService trainingResourceService;

    @Autowired
    private VocabularyService vocabularyService;

    @Autowired
    private FacetLabelService facetLabelService;

    @Value("${catalogue.id}")
    private String catalogueId;
    private final IdCreator idCreator;

    public VocabularyCurationManager(@Lazy RegistrationMailService registrationMailService,
                                     ProviderService providerService,
                                     ServiceBundleService<ServiceBundle> serviceBundleService,
                                     TrainingResourceService trainingResourceService,
                                     IdCreator idCreator) {
        super(VocabularyCuration.class);
        this.registrationMailService = registrationMailService;
        this.providerService = providerService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.idCreator = idCreator;
    }

    @Override
    public String getResourceTypeName() {
        return "vocabulary_curation";
    }

    @Override
    public VocabularyCuration add(VocabularyCuration vocabularyCuration, String resourceType, Authentication auth) {
        vocabularyCuration.setId(idCreator.generate(getResourceTypeName()));
        // set status, dateOfRequest, userId
        vocabularyCuration.setStatus(VocabularyCuration.Status.PENDING.getKey());
        vocabularyCuration.setRejectionReason(null);
        vocabularyCuration.setResolutionDate(null);
        vocabularyCuration.setResolutionUser(null);
        for (VocabularyEntryRequest vocEntryRequest : vocabularyCuration.getVocabularyEntryRequests()) {
            vocEntryRequest.setDateOfRequest(String.valueOf(System.currentTimeMillis()));
            vocEntryRequest.setUserId(AuthenticationInfo.getEmail(auth).toLowerCase());
        }
        // if vocabularyCuration doesn't exist
        validate(vocabularyCuration, resourceType, auth);
        if (vocabularyCuration.getVocabularyEntryRequests().size() == 1) {
            super.add(vocabularyCuration, auth);
            logger.info("Adding Vocabulary Curation: {}", vocabularyCuration);
            registrationMailService.sendEmailsForVocabularyCurationCreation(vocabularyCuration,
                    Objects.requireNonNull(AuthenticationInfo.getFullName(auth)));
            // if vocabularyCuration already exists in "pending"
        } else {
            update(vocabularyCuration, auth);
        }
        return vocabularyCuration;
    }

    @Override
    public VocabularyCuration update(VocabularyCuration vocabularyCuration, Authentication auth) {
        super.update(vocabularyCuration, auth);
        logger.debug("Updating Vocabulary Curation {}", vocabularyCuration);
        return vocabularyCuration;
    }

    private VocabularyCuration validate(VocabularyCuration vocabularyCuration, String resourceType, Authentication auth) {
        // check if vocabulary already exists
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<Vocabulary> allVocs = vocabularyService.getAll(ff, null).getResults();
        List<String> allVocsIds = new ArrayList<>();
        for (Vocabulary vocabulary : allVocs) {
            allVocsIds.add(vocabulary.getName());
        }
        if (allVocsIds.contains(vocabularyCuration.getEntryValueName())) {
            throw new ResourceAlreadyExistsException("Vocabulary with name " + vocabularyCuration.getEntryValueName() + " already exists.");
        }

        // check if vocabularyCuration already exists in "pending"
        List<VocabularyCuration> allVocabularyCurations = getAll(ff, auth).getResults();
        for (VocabularyCuration vocCuration : allVocabularyCurations) {
            if (vocCuration.getEntryValueName().equalsIgnoreCase(vocabularyCuration.getEntryValueName()) &&
                    vocCuration.getVocabulary().equalsIgnoreCase(vocabularyCuration.getVocabulary()) &&
                    vocCuration.getStatus().equals(VocabularyCuration.Status.PENDING.getKey())) {
                vocabularyCuration.setVocabularyEntryRequests(updateVocabularyRequestsList(vocCuration, vocabularyCuration));
                vocabularyCuration.setId(vocCuration.getId());
            }
        }

        // validate vocabulary
        List<String> possibleValues = new ArrayList<>();
        for (Vocabulary.Type vocab : Vocabulary.Type.values()) {
            possibleValues.add(vocab.getKey().toLowerCase());
        }
        String voc = vocabularyCuration.getVocabulary();
        if (!possibleValues.contains(voc.toLowerCase())) {
            throw new ResourceNotFoundException(voc, "Vocabulary");
        }

        // check if parent exists
        List<Vocabulary> specificVocs;
        List<String> specificVocsIds = new ArrayList<>();
        switch (vocabularyCuration.getVocabulary()) {
            case "CATEGORY":
                if (vocabularyCuration.getParent() == null || vocabularyCuration.getParent().isEmpty()) {
                    throw new ValidationException("Vocabulary " + vocabularyCuration.getVocabulary() + " cannot have an empty parent.");
                } else {
                    specificVocs = vocabularyService.getByType(Vocabulary.Type.SUPERCATEGORY);
                    for (Vocabulary vocabulary : specificVocs) {
                        specificVocsIds.add(vocabulary.getId());
                    }
                    if (!specificVocsIds.contains(vocabularyCuration.getParent())) {
                        throw new CatalogueResourceNotFoundException("Parent vocabulary " + vocabularyCuration.getParent() + " does not exist.");
                    }
                }
                break;
            case "SUBCATEGORY":
                if (vocabularyCuration.getParent() == null || vocabularyCuration.getParent().isEmpty()) {
                    throw new ValidationException("Vocabulary " + vocabularyCuration.getVocabulary() + " cannot have an empty parent.");
                } else {
                    specificVocs = vocabularyService.getByType(Vocabulary.Type.CATEGORY);
                    for (Vocabulary vocabulary : specificVocs) {
                        specificVocsIds.add(vocabulary.getId());
                    }
                    if (!specificVocsIds.contains(vocabularyCuration.getParent())) {
                        throw new CatalogueResourceNotFoundException("Parent vocabulary " + vocabularyCuration.getParent() + " does not exist.");
                    }
                }
                break;
            case "PROVIDER MERIL SCIENTIFIC SUBDOMAIN":
                if (vocabularyCuration.getParent() == null || vocabularyCuration.getParent().isEmpty()) {
                    throw new ValidationException("Vocabulary " + vocabularyCuration.getVocabulary() + " cannot have an empty parent.");
                } else {
                    specificVocs = vocabularyService.getByType(Vocabulary.Type.PROVIDER_MERIL_SCIENTIFIC_DOMAIN);
                    for (Vocabulary vocabulary : specificVocs) {
                        specificVocsIds.add(vocabulary.getId());
                    }
                    if (!specificVocsIds.contains(vocabularyCuration.getParent())) {
                        throw new CatalogueResourceNotFoundException("Parent vocabulary " + vocabularyCuration.getParent() + " does not exist.");
                    }
                }
                break;
            case "SCIENTIFIC SUBDOMAIN":
                if (vocabularyCuration.getParent() == null || vocabularyCuration.getParent().isEmpty()) {
                    throw new ValidationException("Vocabulary " + vocabularyCuration.getVocabulary() + " cannot have an empty parent.");
                } else {
                    specificVocs = vocabularyService.getByType(Vocabulary.Type.SCIENTIFIC_DOMAIN);
                    for (Vocabulary vocabulary : specificVocs) {
                        specificVocsIds.add(vocabulary.getId());
                    }
                    if (!specificVocsIds.contains(vocabularyCuration.getParent())) {
                        throw new CatalogueResourceNotFoundException("Parent vocabulary " + vocabularyCuration.getParent() + " does not exist.");
                    }
                }
                break;
            default:
                vocabularyCuration.setParent(null);
        }

        // validate if providerId/resourceId exists
        ProviderBundle providerBundle = providerService.get(catalogueId, vocabularyCuration.getVocabularyEntryRequests().getFirst().getProviderId(), auth);
        switch (resourceType) {
            case "provider":
                break;
            case "service":
                ServiceBundle serviceBundle = serviceBundleService.get(vocabularyCuration.getVocabularyEntryRequests().getFirst().getResourceId());
                if (!serviceBundle.getService().getResourceOrganisation().equals(providerBundle.getId())) {
                    throw new CatalogueResourceNotFoundException(String.format("Provider with id [%s] does not have a Service with id [%s] registered.",
                            providerBundle.getId(), serviceBundle.getId()));
                }
                break;
            case "training_resource":
                TrainingResourceBundle trainingResourceBundle = trainingResourceService.get(vocabularyCuration.getVocabularyEntryRequests().getFirst().getResourceId(), catalogueId);
                if (!trainingResourceBundle.getTrainingResource().getResourceOrganisation().equals(providerBundle.getId())) {
                    throw new CatalogueResourceNotFoundException(String.format("Provider with id [%s] does not have a Training Resource with id [%s] registered.",
                            providerBundle.getId(), trainingResourceBundle.getId()));
                }
                break;
            default:
                throw new ValidationException("The resourceType you submitted is not supported. Possible resourceType " +
                        "values: ['provider', 'service', 'datasource', 'training_resource']");
        }
        return vocabularyCuration;
    }

    public VocabularyCuration addFront(String resourceId, String providerId, String resourceType,
                                       String entryValueName, String vocabulary, String parent, Authentication auth) {
        List<VocabularyEntryRequest> vocabularyEntryRequests = new ArrayList<>();
        VocabularyEntryRequest vocabularyEntryRequest = new VocabularyEntryRequest();
        VocabularyCuration vocabularyCuration = new VocabularyCuration();
        vocabularyEntryRequest.setResourceType(resourceType);
        vocabularyEntryRequest.setResourceId(resourceId);
        vocabularyEntryRequest.setProviderId(providerId);
        vocabularyEntryRequests.add(vocabularyEntryRequest);
        vocabularyCuration.setVocabularyEntryRequests(vocabularyEntryRequests);
        vocabularyCuration.setEntryValueName(entryValueName);
        vocabularyCuration.setVocabulary(vocabulary);
        vocabularyCuration.setParent(parent);
        add(vocabularyCuration, resourceType, auth);
        return vocabularyCuration;
    }

    public Browsing<VocabularyCuration> getAllVocabularyCurationRequests(FacetFilter ff, Authentication auth) {
        List<String> browseBy = new ArrayList<>();
        browseBy.add("vocabulary");
        browseBy.add("resourceType");
        ff.setBrowseBy(browseBy);

        Browsing<VocabularyCuration> vocabularyCurationBrowsing;

        vocabularyCurationBrowsing = getResults(ff);
        if (!vocabularyCurationBrowsing.getResults().isEmpty() && !vocabularyCurationBrowsing.getFacets().isEmpty()) {
            vocabularyCurationBrowsing.setFacets(facetLabelService.generateLabels(vocabularyCurationBrowsing.getFacets()));
        }
        return vocabularyCurationBrowsing;
    }

    public void approveOrRejectVocabularyCuration(VocabularyCuration vocabularyCuration, boolean approved, String rejectionReason, Authentication authentication) {
        vocabularyCuration.setResolutionUser(AuthenticationInfo.getEmail(authentication).toLowerCase());
        vocabularyCuration.setResolutionDate(String.valueOf(System.currentTimeMillis()));
        logger.info("Updating VocabularyRequest {}", vocabularyCuration.getEntryValueName());
        if (approved) {
            vocabularyCuration.setStatus(VocabularyCuration.Status.APPROVED.getKey());
            createNewVocabulary(vocabularyCuration, authentication);
            registrationMailService.sendEmailsForVocabularyCurationResolve(vocabularyCuration);
        } else {
            vocabularyCuration.setStatus(VocabularyCuration.Status.REJECTED.getKey());
            vocabularyCuration.setRejectionReason(rejectionReason);
            registrationMailService.sendEmailsForVocabularyCurationResolve(vocabularyCuration);
        }
        update(vocabularyCuration, authentication);
    }

    public void createNewVocabulary(VocabularyCuration vocabularyCuration, Authentication authentication) {
        Vocabulary vocabulary = new Vocabulary();
        Map<String, String> extras = new HashMap<>();
        String valueNameFormed = vocabularyCuration.getEntryValueName().replaceAll(" ", "_").toLowerCase();
        String vocabularyFormed = vocabularyCuration.getVocabulary().replaceAll(" ", "_").toLowerCase();
        vocabulary.setId(vocabularyFormed + "-" + valueNameFormed);
        vocabulary.setDescription("Vocabulary submitted by " + vocabularyCuration.getVocabularyEntryRequests().getFirst().getUserId());
        vocabulary.setName(vocabularyCuration.getEntryValueName());
        vocabulary.setType(Vocabulary.Type.valueOf(vocabularyFormed.toUpperCase()).getKey());
        vocabulary.setExtras(extras);
        if (vocabularyCuration.getParent() != null && !vocabularyCuration.getParent().isEmpty()) {
            String parentFormed = vocabularyCuration.getParent().replaceAll(" ", "_").toLowerCase();
            vocabulary.setParentId(parentFormed);
        }
        logger.info("User [{}] is adding a new Vocabulary by resolving the vocabulary request '{}'",
                AuthenticationInfo.getEmail(authentication).toLowerCase(), vocabularyCuration.getId());
        vocabularyService.add(vocabulary, authentication);
    }

    List<VocabularyEntryRequest> updateVocabularyRequestsList(VocabularyCuration existingVocabularyCuration, VocabularyCuration newVocabularyCuration) {
        List<VocabularyEntryRequest> updatedEntryRequests = new ArrayList<>();
        updatedEntryRequests.addAll(newVocabularyCuration.getVocabularyEntryRequests());
        updatedEntryRequests.addAll(existingVocabularyCuration.getVocabularyEntryRequests());
        return updatedEntryRequests;
    }

}
