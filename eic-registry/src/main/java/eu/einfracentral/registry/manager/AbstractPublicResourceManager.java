package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.CatalogueService;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.FacetFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import java.util.*;

public abstract class AbstractPublicResourceManager<T extends Identifiable> extends ResourceManager<T> {

    @Autowired
    private SecurityService securityService;
    @Autowired
    private CatalogueService<CatalogueBundle, Authentication> catalogueService;

    public AbstractPublicResourceManager(Class<T> typeParameterClass) {
        super(typeParameterClass);
    }

    protected List<String> appendCatalogueId(List<String> items, String catalogueId, List<String> allCatalogueIds) {
        Set<String> transformed = new HashSet<>();
        if (items != null && !items.isEmpty()) {
            for (String item : items) {
                if (!item.equals("")) {
                    boolean result = checkIfItemContainsAnyOfTheCatalogueIds(item, allCatalogueIds);
                    if (!result) {
                        item = catalogueId + "." + item;
                    }
                    transformed.add(item);
                }
            }
        }
        return new ArrayList<>(transformed);
    }

    protected void updateResourceIdsToPublic(ServiceBundle serviceBundle) {
        List<String> allCatalogueIds = getAllCatalogueIds();
        // Resource Organisation
        serviceBundle.getService().setResourceOrganisation(
                String.format("%s.%s",
                        serviceBundle.getService().getCatalogueId(),
                        serviceBundle.getService().getResourceOrganisation()));

        // Resource Providers
        serviceBundle.getService().setResourceProviders(
                appendCatalogueId(
                        serviceBundle.getService().getResourceProviders(),
                        serviceBundle.getService().getCatalogueId(),
                        allCatalogueIds));

        // Related Resources
        serviceBundle.getService().setRelatedResources(
                appendCatalogueId(
                        serviceBundle.getService().getRelatedResources(),
                        serviceBundle.getService().getCatalogueId(),
                        allCatalogueIds));

        // Required Resources
        serviceBundle.getService().setRequiredResources(
                appendCatalogueId(
                        serviceBundle.getService().getRequiredResources(),
                        serviceBundle.getService().getCatalogueId(),
                        allCatalogueIds));
    }

    protected void updateTrainingResourceIdsToPublic(TrainingResourceBundle trainingResourceBundle) {
        List<String> allCatalogueIds = getAllCatalogueIds();
        // Resource Organisation
        trainingResourceBundle.getTrainingResource().setResourceOrganisation(
                String.format("%s.%s",
                        trainingResourceBundle.getTrainingResource().getCatalogueId(),
                        trainingResourceBundle.getTrainingResource().getResourceOrganisation()));

        // Resource Providers
        trainingResourceBundle.getTrainingResource().setResourceProviders(
                appendCatalogueId(
                        trainingResourceBundle.getTrainingResource().getResourceProviders(),
                        trainingResourceBundle.getTrainingResource().getCatalogueId(),
                        allCatalogueIds));

        // EOSC Related Services
        trainingResourceBundle.getTrainingResource().setEoscRelatedServices(
                appendCatalogueId(
                        trainingResourceBundle.getTrainingResource().getEoscRelatedServices(),
                        trainingResourceBundle.getTrainingResource().getCatalogueId(),
                        allCatalogueIds));
    }

    protected void updateResourceInteroperabilityRecordIdsToPublic(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle) {
        List<String> allCatalogueIds = getAllCatalogueIds();
        // Resource Organisation
        resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().setResourceId(
                String.format("%s.%s",
                        resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId(),
                        resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getResourceId()));
        // Interoperability Record IDs
        resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().setInteroperabilityRecordIds(
                appendCatalogueId(
                        resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getInteroperabilityRecordIds(),
                        resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId(),
                        allCatalogueIds));
    }

    protected List<String> getAllCatalogueIds() {
        List<String> catalogueIds = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(1000);
        ff.addFilter("status", "approved catalogue");
        ff.addFilter("active", "true");
        List<CatalogueBundle> allCatalogues = catalogueService.getAll(ff, securityService.getAdminAccess()).getResults();
        for (CatalogueBundle catalogueBundle : allCatalogues) {
            catalogueIds.add(catalogueBundle.getId());
        }
        return catalogueIds;
    }

    protected boolean checkIfItemContainsAnyOfTheCatalogueIds(String item, List<String> allCatalogueIds) {
        boolean containsCatalogueId = false;
        String[] parts = item.split("\\.");
        for (String catalogueId : allCatalogueIds) {
            if (parts[0].equals(catalogueId)) {
                containsCatalogueId = true;
                break;
            }
        }
        return containsCatalogueId;
    }
}
