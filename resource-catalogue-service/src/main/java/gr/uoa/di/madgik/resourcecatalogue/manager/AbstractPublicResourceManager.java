package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.CatalogueService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.utils.PublicResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractPublicResourceManager<T extends Identifiable> extends ResourceManager<T> {

    @Autowired
    private SecurityService securityService;
    @Autowired
    private CatalogueService catalogueService;
    @Autowired
    private PublicResourceUtils publicResourceUtils;

    @Value("${catalogue.id}")
    private String catalogueId;

    public AbstractPublicResourceManager(Class<T> typeParameterClass) {
        super(typeParameterClass);
    }

    protected void updateServiceIdsToPublic(ServiceBundle serviceBundle) {
        List<String> allCatalogueIds = getAllCatalogueIds();
        // Resource Organisation
        serviceBundle.getService().setResourceOrganisation(publicResourceUtils.createPublicResourceId(
                serviceBundle.getService().getResourceOrganisation(), serviceBundle.getService().getCatalogueId()));

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
        datasourceBundle.getDatasource().setServiceId(publicResourceUtils.createPublicResourceId(
                datasourceBundle.getDatasource().getServiceId(), datasourceBundle.getDatasource().getCatalogueId()));
    }

    protected void updateHelpdeskIdsToPublic(HelpdeskBundle helpdeskBundle) {
        // serviceId
        helpdeskBundle.getHelpdesk().setServiceId(publicResourceUtils.createPublicResourceId(
                helpdeskBundle.getHelpdesk().getServiceId(), helpdeskBundle.getCatalogueId()));
    }

    protected void updateMonitoringIdsToPublic(MonitoringBundle monitoringBundle) {
        // serviceId
        monitoringBundle.getMonitoring().setServiceId(publicResourceUtils.createPublicResourceId(
                monitoringBundle.getMonitoring().getServiceId(), monitoringBundle.getCatalogueId()));
    }

    protected void updateTrainingResourceIdsToPublic(TrainingResourceBundle trainingResourceBundle) {
        List<String> allCatalogueIds = getAllCatalogueIds();
        // Resource Organisation
        trainingResourceBundle.getTrainingResource().setResourceOrganisation(publicResourceUtils.createPublicResourceId(
                trainingResourceBundle.getTrainingResource().getResourceOrganisation(),
                trainingResourceBundle.getTrainingResource().getCatalogueId()));

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

    protected void updateInteroperabilityRecordIdsToPublic(InteroperabilityRecordBundle interoperabilityRecordBundle) {
        // providerId
        interoperabilityRecordBundle.getInteroperabilityRecord().setProviderId(publicResourceUtils.createPublicResourceId(
                interoperabilityRecordBundle.getInteroperabilityRecord().getProviderId(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId()));
    }

    protected void updateResourceInteroperabilityRecordIdsToPublic(ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle) {
        List<String> allCatalogueIds = getAllCatalogueIds();
        // resourceId
        resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().setResourceId(publicResourceUtils.createPublicResourceId(
                resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getResourceId(),
                resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId()));
        // Interoperability Record IDs
        resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().setInteroperabilityRecordIds(
                appendCatalogueId(
                        resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getInteroperabilityRecordIds(),
                        resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getCatalogueId(),
                        allCatalogueIds));
    }

    //TODO: Refactor if CTIs can belong to a different from the Project's Catalogue
    protected void updateConfigurationTemplateInstanceIdsToPublic(ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle) {
        // resourceId
        configurationTemplateInstanceBundle.getConfigurationTemplateInstance().setResourceId(publicResourceUtils.createPublicResourceId(
                configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getResourceId(), catalogueId));
        //TODO: enable if we have public CT
        // configurationTemplateId
//        configurationTemplateInstanceBundle.getConfigurationTemplateInstance().setResourceId(publicResourceUtils.createPublicResourceId(
//                configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getConfigurationTemplateId(), catalogueId));
    }

    protected List<String> appendCatalogueId(List<String> items, String catalogueId, List<String> allCatalogueIds) {
        Set<String> transformed = new HashSet<>();
        if (items != null && !items.isEmpty()) {
            for (String item : items) {
                if (!item.equals("")) {
                    //TODO: test if we need checkIfItemContainsAnyOfTheCatalogueIds()
                    boolean result = checkIfItemContainsAnyOfTheCatalogueIds(item, allCatalogueIds);
                    if (!result) {
                        item = publicResourceUtils.createPublicResourceId(item, catalogueId);
                    }
                    transformed.add(item);
                }
            }
        }
        return new ArrayList<>(transformed);
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
