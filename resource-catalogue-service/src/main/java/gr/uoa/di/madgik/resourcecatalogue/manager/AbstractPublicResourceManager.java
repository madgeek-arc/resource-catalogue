package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.CatalogueService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractPublicResourceManager<T extends Identifiable> extends ResourceManager<T> {

    @Autowired
    private SecurityService securityService;
    @Autowired
    private CatalogueService catalogueService;

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

    protected void updateServiceIdsToPublic(ServiceBundle serviceBundle) {
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

    protected void updateDatasourceIdsToPublic(DatasourceBundle datasourceBundle) {
        // serviceId
        datasourceBundle.getDatasource().setServiceId(
                String.format("%s.%s",
                        datasourceBundle.getDatasource().getCatalogueId(),
                        datasourceBundle.getDatasource().getServiceId()));
    }

    protected void updateHelpdeskIdsToPublic(HelpdeskBundle helpdeskBundle) {
        // serviceId
        helpdeskBundle.getHelpdesk().setServiceId(
                String.format("%s.%s",
                        helpdeskBundle.getCatalogueId(),
                        helpdeskBundle.getHelpdesk().getServiceId()));
    }

    protected void updateMonitoringIdsToPublic(MonitoringBundle monitoringBundle) {
        // serviceId
        monitoringBundle.getMonitoring().setServiceId(
                String.format("%s.%s",
                        monitoringBundle.getCatalogueId(),
                        monitoringBundle.getMonitoring().getServiceId()));
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
