package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.domain.ServiceBundle;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.*;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.search.SearchServiceEIC;
import eu.einfracentral.utils.FacetLabelService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.index.IndexField;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;


@Component
public class VocabularyCurationManager extends ResourceManager<VocabularyCuration> implements VocabularyCurationService<VocabularyCuration, Authentication> {

    private static final Logger logger = LogManager.getLogger(VocabularyCurationManager.class);
    private final RegistrationMailService registrationMailService;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService<TrainingResourceBundle> trainingResourceService;
    private List<String> browseBy;
    private Map<String, String> labels;

    @Autowired
    private VocabularyService vocabularyService;

    @Autowired
    private FacetLabelService facetLabelService;

    private final AbstractServiceBundleManager<ServiceBundle> abstractServiceBundleManager;

    @Autowired
    private SearchServiceEIC searchServiceEIC;

    @Value("${project.catalogue.name}")
    private String catalogueName;


    @Autowired
    public VocabularyCurationManager(@Lazy RegistrationMailService registrationMailService, ProviderService providerService,
                                     ServiceBundleService<ServiceBundle> serviceBundleService,
                                     TrainingResourceService<TrainingResourceBundle> trainingResourceService,
                                     AbstractServiceBundleManager<ServiceBundle> abstractServiceBundleManager) {
        super(VocabularyCuration.class);
        this.registrationMailService = registrationMailService;
        this.providerService = providerService;
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.abstractServiceBundleManager = abstractServiceBundleManager;
    }

    @PostConstruct
    void initLabels() {
        resourceType = resourceTypeService.getResourceType(getResourceType());
        Set<String> browseSet = new HashSet<>();
        Map<String, Set<String>> sets = new HashMap<>();
        labels = new HashMap<>();
        labels.put("resourceType", "Resource Type");
        for (IndexField f : resourceTypeService.getResourceTypeIndexFields(getResourceType())) {
            sets.putIfAbsent(f.getResourceType().getName(), new HashSet<>());
            labels.put(f.getName(), f.getLabel());
            if (f.getLabel() != null) {
                sets.get(f.getResourceType().getName()).add(f.getName());
            }
        }
        boolean flag = true;
        for (Map.Entry<String, Set<String>> entry : sets.entrySet()) {
            if (flag) {
                browseSet.addAll(entry.getValue());
                flag = false;
            } else {
                browseSet.retainAll(entry.getValue());
            }
        }
        browseBy = new ArrayList<>();
        browseBy.addAll(browseSet);
        browseBy.add("resourceType");
        java.util.Collections.sort(browseBy);
        logger.info("Generated generic service for '{}'[{}]", getResourceType(), getClass().getSimpleName());
    }

    @Override
    public String getResourceType() {
        return "vocabulary_curation";
    }

    @Override
    public VocabularyCuration add(VocabularyCuration vocabularyCuration, String resourceType, Authentication auth) {
        if ((vocabularyCuration.getId() == null) || vocabularyCuration.getId().equals("")) {
            vocabularyCuration.setId(UUID.randomUUID().toString());
        } else {
            throw new ValidationException("You must not provide a VocabularyCuration id");
        }
        // set status, dateOfRequest, userId
        vocabularyCuration.setStatus(VocabularyCuration.Status.PENDING.getKey());
        vocabularyCuration.setRejectionReason(null);
        vocabularyCuration.setResolutionDate(null);
        vocabularyCuration.setResolutionUser(null);
        for (VocabularyEntryRequest vocEntryRequest : vocabularyCuration.getVocabularyEntryRequests()) {
            vocEntryRequest.setDateOfRequest(now());
            vocEntryRequest.setUserId(((OIDCAuthenticationToken) auth).getUserInfo().getEmail());
        }
        // if vocabularyCuration doesn't exist
        validate(vocabularyCuration, resourceType, auth);
        if (vocabularyCuration.getVocabularyEntryRequests().size() == 1) {
            super.add(vocabularyCuration, auth);
            logger.info("Adding Vocabulary Curation: {}", vocabularyCuration);
            registrationMailService.sendVocabularyCurationEmails(vocabularyCuration, ((OIDCAuthenticationToken) auth).getUserInfo().getName());
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
            throw new ValidationException("Vocabulary with name " + vocabularyCuration.getEntryValueName() + " already exists.");
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
            throw new ValidationException("Vocabulary " + voc + "' does not exist.");
        }

        // check if parent exists
        List<Vocabulary> specificVocs;
        List<String> specificVocsIds = new ArrayList<>();
        switch (vocabularyCuration.getVocabulary()) {
            case "CATEGORY":
                if (vocabularyCuration.getParent() == null || vocabularyCuration.getParent().equals("")) {
                    throw new ValidationException("Vocabulary " + vocabularyCuration.getVocabulary() + " cannot have an empty parent.");
                } else {
                    specificVocs = vocabularyService.getByType(Vocabulary.Type.SUPERCATEGORY);
                    for (Vocabulary vocabulary : specificVocs) {
                        specificVocsIds.add(vocabulary.getId());
                    }
                    if (!specificVocsIds.contains(vocabularyCuration.getParent())) {
                        throw new ValidationException("Parent vocabulary " + vocabularyCuration.getParent() + " does not exist.");
                    }
                }
                break;
            case "SUBCATEGORY":
                if (vocabularyCuration.getParent() == null || vocabularyCuration.getParent().equals("")) {
                    throw new ValidationException("Vocabulary " + vocabularyCuration.getVocabulary() + " cannot have an empty parent.");
                } else {
                    specificVocs = vocabularyService.getByType(Vocabulary.Type.CATEGORY);
                    for (Vocabulary vocabulary : specificVocs) {
                        specificVocsIds.add(vocabulary.getId());
                    }
                    if (!specificVocsIds.contains(vocabularyCuration.getParent())) {
                        throw new ValidationException("Parent vocabulary " + vocabularyCuration.getParent() + " does not exist.");
                    }
                }
                break;
            case "PROVIDER MERIL SCIENTIFIC SUBDOMAIN":
                if (vocabularyCuration.getParent() == null || vocabularyCuration.getParent().equals("")) {
                    throw new ValidationException("Vocabulary " + vocabularyCuration.getVocabulary() + " cannot have an empty parent.");
                } else {
                    specificVocs = vocabularyService.getByType(Vocabulary.Type.PROVIDER_MERIL_SCIENTIFIC_DOMAIN);
                    for (Vocabulary vocabulary : specificVocs) {
                        specificVocsIds.add(vocabulary.getId());
                    }
                    if (!specificVocsIds.contains(vocabularyCuration.getParent())) {
                        throw new ValidationException("Parent vocabulary " + vocabularyCuration.getParent() + " does not exist.");
                    }
                }
                break;
            case "SCIENTIFIC SUBDOMAIN":
                if (vocabularyCuration.getParent() == null || vocabularyCuration.getParent().equals("")) {
                    throw new ValidationException("Vocabulary " + vocabularyCuration.getVocabulary() + " cannot have an empty parent.");
                } else {
                    specificVocs = vocabularyService.getByType(Vocabulary.Type.SCIENTIFIC_DOMAIN);
                    for (Vocabulary vocabulary : specificVocs) {
                        specificVocsIds.add(vocabulary.getId());
                    }
                    if (!specificVocsIds.contains(vocabularyCuration.getParent())) {
                        throw new ValidationException("Parent vocabulary " + vocabularyCuration.getParent() + " does not exist.");
                    }
                }
                break;
            default:
                vocabularyCuration.setParent(null);
        }

        // validate if providerId/resourceId exists
        ProviderBundle providerBundle = providerService.get(catalogueName, vocabularyCuration.getVocabularyEntryRequests().get(0).getProviderId(), auth);
        switch (resourceType) {
            case "provider":
                break;
            case "service":
                ServiceBundle serviceBundle = serviceBundleService.get(vocabularyCuration.getVocabularyEntryRequests().get(0).getResourceId(), catalogueName);
                if (!serviceBundle.getService().getResourceOrganisation().equals(providerBundle.getId())) {
                    throw new ValidationException(String.format("Provider with id [%s] does not have a Service with id [%s] registered.", providerBundle.getId(), serviceBundle.getId()));
                }
                break;
            case "training_resource":
                TrainingResourceBundle trainingResourceBundle = trainingResourceService.get(vocabularyCuration.getVocabularyEntryRequests().get(0).getResourceId(), catalogueName);
                if (!trainingResourceBundle.getTrainingResource().getResourceOrganisation().equals(providerBundle.getId())) {
                    throw new ValidationException(String.format("Provider with id [%s] does not have a Training Resource with id [%s] registered.", providerBundle.getId(), trainingResourceBundle.getId()));
                }
                break;
            default:
                throw new ValidationException("The resourceType you submitted is not supported. Possible resourceType values: ['provider', 'service', 'datasource', 'training_resource']");
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

    public Date now() {
        return DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
    }

    public Browsing<VocabularyCuration> getAllVocabularyCurationRequests(FacetFilter ff, Authentication auth) {
        List<String> orderedBrowseBy = new ArrayList<>();
        browseBy.add("vocabulary");
        orderedBrowseBy.add(browseBy.get(0));    // resourceType
        orderedBrowseBy.add(browseBy.get(1));    // vocabulary

        ff.setBrowseBy(orderedBrowseBy);

        Browsing<VocabularyCuration> vocabularyCurationBrowsing;

        vocabularyCurationBrowsing = getResults(ff);
        if (!vocabularyCurationBrowsing.getResults().isEmpty() && !vocabularyCurationBrowsing.getFacets().isEmpty()) {
            vocabularyCurationBrowsing.setFacets(facetLabelService.createLabels(vocabularyCurationBrowsing.getFacets()));
        }
        return vocabularyCurationBrowsing;
    }

    public void approveOrRejectVocabularyCuration(VocabularyCuration vocabularyCuration, boolean approved, String rejectionReason, Authentication authentication) {
        vocabularyCuration.setResolutionUser(User.of(authentication).getEmail());
        vocabularyCuration.setResolutionDate(now());
        logger.info("Updating VocabularyRequest " + vocabularyCuration.getEntryValueName());
        if (approved) {
            vocabularyCuration.setStatus(VocabularyCuration.Status.APPROVED.getKey());
            createNewVocabulary(vocabularyCuration, authentication);
            registrationMailService.approveOrRejectVocabularyCurationEmails(vocabularyCuration);
        } else {
            vocabularyCuration.setStatus(VocabularyCuration.Status.REJECTED.getKey());
            vocabularyCuration.setRejectionReason(rejectionReason);
            registrationMailService.approveOrRejectVocabularyCurationEmails(vocabularyCuration);
        }
        update(vocabularyCuration, authentication);
    }

    public void createNewVocabulary(VocabularyCuration vocabularyCuration, Authentication authentication) {
        Vocabulary vocabulary = new Vocabulary();
        Map<String, String> extras = new HashMap<>();
        String valueNameFormed = vocabularyCuration.getEntryValueName().replaceAll(" ", "_").toLowerCase();
        String vocabularyFormed = vocabularyCuration.getVocabulary().replaceAll(" ", "_").toLowerCase();
        vocabulary.setId(vocabularyFormed + "-" + valueNameFormed);
        vocabulary.setDescription("Vocabulary submitted by " + vocabularyCuration.getVocabularyEntryRequests().get(0).getUserId());
        vocabulary.setName(vocabularyCuration.getEntryValueName());
        vocabulary.setType(Vocabulary.Type.valueOf(vocabularyFormed.toUpperCase()).getKey());
        vocabulary.setExtras(extras);
        if (vocabularyCuration.getParent() != null && !vocabularyCuration.getParent().equals("")) {
            String parentFormed = vocabularyCuration.getParent().replaceAll(" ", "_").toLowerCase();
            vocabulary.setParentId(parentFormed);
        }
        logger.info("User " + User.of(authentication).getEmail() + " is adding a new Vocabulary by resolving the vocabulary request " + vocabularyCuration.getId());
        vocabularyService.add(vocabulary, authentication);
    }

    List<VocabularyEntryRequest> updateVocabularyRequestsList(VocabularyCuration existingVocabularyCuration, VocabularyCuration newVocabularyCuration) {
        List<VocabularyEntryRequest> updatedEntryRequests = new ArrayList<>();
        updatedEntryRequests.addAll(newVocabularyCuration.getVocabularyEntryRequests());
        updatedEntryRequests.addAll(existingVocabularyCuration.getVocabularyEntryRequests());
        return updatedEntryRequests;
    }

    @Override
    protected Browsing<VocabularyCuration> getResults(FacetFilter filter) {
        Browsing<VocabularyCuration> browsing;
        filter.setResourceType(getResourceType());
        browsing = convertToBrowsingEIC(searchServiceEIC.search(filter));

        browsing.setFacets(abstractServiceBundleManager.createCorrectFacets(browsing.getFacets(), filter));
        return browsing;
    }

    private Browsing<VocabularyCuration> convertToBrowsingEIC(@NotNull Paging<Resource> paging) {
        List<VocabularyCuration> results = paging.getResults()
                .stream()
                .map(res -> parserPool.deserialize(res, typeParameterClass))
                .collect(Collectors.toList());
        return new Browsing<>(paging, results, labels);
    }

}
