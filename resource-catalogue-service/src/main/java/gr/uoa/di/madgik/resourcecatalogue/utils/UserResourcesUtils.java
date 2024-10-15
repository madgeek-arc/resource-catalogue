package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserResourcesUtils {

    private Map<String, Map<String, List<String>>> userEmailToResources = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(UserResourcesUtils.class);

    private final SecurityService securityService;
    private final CatalogueService catalogueService;
    private final ProviderService providerService;
    private final DraftResourceService<ProviderBundle> draftProviderService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final DraftResourceService<ServiceBundle> draftServiceService;
    private final TrainingResourceService trainingResourceService;
    private final DraftResourceService<TrainingResourceBundle> draftTrainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final DraftResourceService<InteroperabilityRecordBundle> draftInteroperabilityRecordService;

    public UserResourcesUtils(SecurityService securityService,
                              CatalogueService catalogueService,
                              ProviderService providerService,
                              DraftResourceService<ProviderBundle> draftProviderService,
                              ServiceBundleService<ServiceBundle> serviceBundleService,
                              DraftResourceService<ServiceBundle> draftServiceService,
                              TrainingResourceService trainingResourceService,
                              DraftResourceService<TrainingResourceBundle> draftTrainingResourceService,
                              InteroperabilityRecordService interoperabilityRecordService,
                              DraftResourceService<InteroperabilityRecordBundle> draftInteroperabilityRecordService) {
        this.securityService = securityService;
        this.catalogueService = catalogueService;
        this.providerService = providerService;
        this.draftProviderService = draftProviderService;
        this.serviceBundleService = serviceBundleService;
        this.draftServiceService = draftServiceService;
        this.trainingResourceService = trainingResourceService;
        this.draftTrainingResourceService = draftTrainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.draftInteroperabilityRecordService = draftInteroperabilityRecordService;
    }

    //TODO: populate
    @PostConstruct
    public void createUserResourcesRelation() {
        createUserCatalogueRelation();
        createUserProviderRelation();
        createUserDraftProviderRelation();
        createUserServiceRelation();
        createUserDraftServiceRelation();
        createUserTrainingResourceRelation();
        createUserDraftTrainingResourceRelation();
        createUserInteroperabilityRecordRelation();
        createUserDraftInteroperabilityRecordRelation();
        logger.info("asdf");
    }

    private void createUserCatalogueRelation() {
        List<CatalogueBundle> catalogueBundles = catalogueService.getAll(
                createFacetFilter("catalogue"), securityService.getAdminAccess()).getResults();
        for (CatalogueBundle catalogueBundle : catalogueBundles) {
            List<User> users = catalogueBundle.getCatalogue().getUsers();
            for (User user : users) {
                userEmailToResources.putIfAbsent(user.getEmail(), new HashMap<>());
                Map<String, List<String>> resourceTypeToResources = userEmailToResources.get(user.getEmail());
                resourceTypeToResources.putIfAbsent("catalogues", new ArrayList<>());
                resourceTypeToResources.get("catalogues").add(catalogueBundle.getId());
            }
        }
    }

    private void createUserProviderRelation() {
        List<ProviderBundle> providerBundles = providerService.getAll(
                createFacetFilter("provider"), securityService.getAdminAccess()).getResults();
        for (ProviderBundle providerBundle : providerBundles) {
            List<User> users = providerBundle.getProvider().getUsers();
            for (User user : users) {
                userEmailToResources.putIfAbsent(user.getEmail(), new HashMap<>());
                Map<String, List<String>> resourceTypeToResources = userEmailToResources.get(user.getEmail());
                resourceTypeToResources.putIfAbsent("providers", new ArrayList<>());
                resourceTypeToResources.get("providers").add(providerBundle.getId());
            }
        }
    }

    private void createUserDraftProviderRelation() {
        List<ProviderBundle> providerBundles = draftProviderService.getAll(
                createFacetFilter("draft_provider"), securityService.getAdminAccess()).getResults();
        for (ProviderBundle providerBundle : providerBundles) {
            List<User> users = providerBundle.getProvider().getUsers();
            for (User user : users) {
                userEmailToResources.putIfAbsent(user.getEmail(), new HashMap<>());
                Map<String, List<String>> resourceTypeToResources = userEmailToResources.get(user.getEmail());
                resourceTypeToResources.putIfAbsent("draft_providers", new ArrayList<>());
                resourceTypeToResources.get("draft_providers").add(providerBundle.getId());
            }
        }
    }

    private void createUserServiceRelation() {
        List<ServiceBundle> serviceBundles = serviceBundleService.getAll(
                createFacetFilter("service"), securityService.getAdminAccess()).getResults();
        for (ServiceBundle serviceBundle : serviceBundles) {
            String resourceOrganisation = serviceBundle.getService().getResourceOrganisation();
            for (Map.Entry<String, Map<String, List<String>>> entry : userEmailToResources.entrySet()) {
                Map<String, List<String>> resourceTypeToResources = entry.getValue();
                if (resourceTypeToResources.get("providers").contains(resourceOrganisation)) {
                    resourceTypeToResources.putIfAbsent("services", new ArrayList<>());
                    resourceTypeToResources.get("services").add(serviceBundle.getId());
                }
            }
        }
    }

    private void createUserDraftServiceRelation() {
        List<ServiceBundle> serviceBundles = draftServiceService.getAll(
                createFacetFilter("draft_service"), securityService.getAdminAccess()).getResults();
        for (ServiceBundle serviceBundle : serviceBundles) {
            String resourceOrganisation = serviceBundle.getService().getResourceOrganisation();
            for (Map.Entry<String, Map<String, List<String>>> entry : userEmailToResources.entrySet()) {
                Map<String, List<String>> resourceTypeToResources = entry.getValue();
                if (resourceTypeToResources.get("providers").contains(resourceOrganisation)) {
                    resourceTypeToResources.putIfAbsent("draft_services", new ArrayList<>());
                    resourceTypeToResources.get("draft_services").add(serviceBundle.getId());
                }
            }
        }
    }

    private void createUserTrainingResourceRelation() {
        List<TrainingResourceBundle> trainingResourceBundles = trainingResourceService.getAll(
                createFacetFilter("training_resource"), securityService.getAdminAccess()).getResults();
        for (TrainingResourceBundle trainingResourceBundle : trainingResourceBundles) {
            String resourceOrganisation = trainingResourceBundle.getTrainingResource().getResourceOrganisation();
            for (Map.Entry<String, Map<String, List<String>>> entry : userEmailToResources.entrySet()) {
                Map<String, List<String>> resourceTypeToResources = entry.getValue();
                if (resourceTypeToResources.get("providers").contains(resourceOrganisation)) {
                    resourceTypeToResources.putIfAbsent("training_resources", new ArrayList<>());
                    resourceTypeToResources.get("training_resources").add(trainingResourceBundle.getId());
                }
            }
        }
    }

    private void createUserDraftTrainingResourceRelation() {
        List<TrainingResourceBundle> trainingResourceBundles = draftTrainingResourceService.getAll(
                createFacetFilter("draft_training_resource"), securityService.getAdminAccess()).getResults();
        for (TrainingResourceBundle trainingResourceBundle : trainingResourceBundles) {
            String resourceOrganisation = trainingResourceBundle.getTrainingResource().getResourceOrganisation();
            for (Map.Entry<String, Map<String, List<String>>> entry : userEmailToResources.entrySet()) {
                Map<String, List<String>> resourceTypeToResources = entry.getValue();
                if (resourceTypeToResources.get("providers").contains(resourceOrganisation)) {
                    resourceTypeToResources.putIfAbsent("draft_training_resources", new ArrayList<>());
                    resourceTypeToResources.get("draft_training_resources").add(trainingResourceBundle.getId());
                }
            }
        }
    }

    private void createUserInteroperabilityRecordRelation() {
        List<InteroperabilityRecordBundle> interoperabilityRecordBundles = interoperabilityRecordService.getAll(
                createFacetFilter("interoperability_record"), securityService.getAdminAccess()).getResults();
        for (InteroperabilityRecordBundle interoperabilityRecordBundle : interoperabilityRecordBundles) {
            String resourceOrganisation = interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId();
            for (Map.Entry<String, Map<String, List<String>>> entry : userEmailToResources.entrySet()) {
                Map<String, List<String>> resourceTypeToResources = entry.getValue();
                if (resourceTypeToResources.get("providers").contains(resourceOrganisation)) {
                    resourceTypeToResources.putIfAbsent("interoperability_records", new ArrayList<>());
                    resourceTypeToResources.get("interoperability_records").add(interoperabilityRecordBundle.getId());
                }
            }
        }
    }

    private void createUserDraftInteroperabilityRecordRelation() {
        List<InteroperabilityRecordBundle> interoperabilityRecordBundles = draftInteroperabilityRecordService.getAll(
                createFacetFilter("draft_interoperability_record"), securityService.getAdminAccess()).getResults();
        for (InteroperabilityRecordBundle interoperabilityRecordBundle : interoperabilityRecordBundles) {
            String resourceOrganisation = interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId();
            for (Map.Entry<String, Map<String, List<String>>> entry : userEmailToResources.entrySet()) {
                Map<String, List<String>> resourceTypeToResources = entry.getValue();
                if (resourceTypeToResources.get("providers").contains(resourceOrganisation)) {
                    resourceTypeToResources.putIfAbsent("draft_interoperability_records", new ArrayList<>());
                    resourceTypeToResources.get("draft_interoperability_records").add(interoperabilityRecordBundle.getId());
                }
            }
        }
    }

    private FacetFilter createFacetFilter(String resourceType) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.setResourceType(resourceType);
        return ff;
    }
}
