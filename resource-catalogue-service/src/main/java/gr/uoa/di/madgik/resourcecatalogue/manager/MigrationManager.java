package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.ResourceService;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.MigrationService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MigrationManager implements MigrationService {

    private static final Logger logger = LoggerFactory.getLogger(MigrationManager.class);

    private final ServiceBundleManager serviceBundleManager;
    private final PublicServiceManager publicServiceManager;
    private final TrainingResourceManager trainingResourceManager;
    private final InteroperabilityRecordManager interoperabilityRecordManager;
    private final DatasourceManager datasourceManager;
    private final PublicTrainingResourceManager publicTrainingResourceManager;
    private final ProviderManager providerService;
    private final ResourceService resourceService;
    private final ResourceInteroperabilityRecordManager resourceInteroperabilityRecordManager;
    private final PublicResourceInteroperabilityRecordManager publicResourceInteroperabilityRecordManager;
    private final HelpdeskManager helpdeskManager;
    private final MonitoringManager monitoringManager;
    private final JmsService jmsService;
    private final SecurityService securityService;

    @Value("${catalogue.id}")
    private String catalogueId;
    @Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    public MigrationManager(ServiceBundleManager serviceBundleManager, PublicServiceManager publicServiceManager,
                            TrainingResourceManager trainingResourceManager, DatasourceManager datasourceManager,
                            InteroperabilityRecordManager interoperabilityRecordManager,
                            PublicTrainingResourceManager publicTrainingResourceManager,
                            ProviderManager providerService, ResourceService resourceService,
                            ResourceInteroperabilityRecordManager resourceInteroperabilityRecordManager,
                            PublicResourceInteroperabilityRecordManager publicResourceInteroperabilityRecordManager,
                            HelpdeskManager helpdeskManager, MonitoringManager monitoringManager,
                            JmsService jmsService, SecurityService securityService) {
        this.serviceBundleManager = serviceBundleManager;
        this.publicServiceManager = publicServiceManager;
        this.trainingResourceManager = trainingResourceManager;
        this.datasourceManager = datasourceManager;
        this.interoperabilityRecordManager = interoperabilityRecordManager;
        this.publicTrainingResourceManager = publicTrainingResourceManager;
        this.providerService = providerService;
        this.resourceService = resourceService;
        this.resourceInteroperabilityRecordManager = resourceInteroperabilityRecordManager;
        this.publicResourceInteroperabilityRecordManager = publicResourceInteroperabilityRecordManager;
        this.helpdeskManager = helpdeskManager;
        this.monitoringManager = monitoringManager;
        this.jmsService = jmsService;
        this.securityService = securityService;
    }

    public ProviderBundle changeProviderCatalogue(String providerId, String catalogueId, String newCatalogueId, Authentication authentication) {
        logger.info("User [{}] is updating the catalogueId of the Provider [{}] and all its Resources to [{}]",
                User.of(authentication).getFullName(), providerId, newCatalogueId);
        // Provider
        ProviderBundle providerBundle = providerService.get(catalogueId, providerId, authentication);
        providerBundle.getProvider().setCatalogueId(newCatalogueId);

        Resource resource = providerService.getResource(providerBundle.getId(), catalogueId);
        resource.setPayload(providerService.serialize(providerBundle));
        logger.debug("Migrating Provider: {} of Catalogue: {} to Catalogue: {}", providerBundle.getId(), catalogueId, newCatalogueId);
        resourceService.updateResource(resource);

        // Public Provider
        try {
            ProviderBundle publicProviderBundle = providerService.get(catalogueId, catalogueId + "." + providerId, authentication);
            String oldPublicId = publicProviderBundle.getProvider().getId();
            publicProviderBundle.getProvider().setId(newCatalogueId + "." + providerId);
            publicProviderBundle.getProvider().setCatalogueId(newCatalogueId);

            Resource publicResource = providerService.getResource(oldPublicId, catalogueId);
            publicResource.setPayload(providerService.serialize(publicProviderBundle));
            logger.info("Migrating Public Provider: {} from Catalogue: {} to Catalogue: {}", publicProviderBundle.getId(), catalogueId, newCatalogueId);
            resourceService.updateResource(publicResource);
            jmsService.convertAndSendTopic("provider.update", publicProviderBundle);
        } catch (RuntimeException e) {
            logger.error("Error migrating Public Provider", e);
        }

        // Update provider's resources' catalogue
        changeServiceCatalogue(providerId, catalogueId, newCatalogueId, authentication);
        changeTrainingResourceCatalogue(providerId, catalogueId, newCatalogueId, authentication);
        changeInteroperabilityRecordCatalogue(providerId, catalogueId, newCatalogueId, authentication);

        return providerBundle;
    }

    private void changeServiceCatalogue(String providerId, String catalogueId, String newCatalogueId, Authentication authentication) {
        List<ServiceBundle> serviceBundles = serviceBundleManager.getResourceBundles(catalogueId, providerId, authentication).getResults();
        // Services
        String jmsTopic = "service.update";
        boolean sendJMS;
        for (ServiceBundle serviceBundle : serviceBundles) {
            sendJMS = false;
            String oldResourceId = serviceBundle.getId();
            if (serviceBundle.getService().getId().startsWith(catalogueId)) {
                // if Service is Public, update its id
                sendJMS = true;
                String id = serviceBundle.getId().replaceFirst(catalogueId, newCatalogueId);
                serviceBundle.getService().setId(id);
            }
            serviceBundle.getService().setCatalogueId(newCatalogueId);
            Resource resource = serviceBundleManager.getResource(oldResourceId, catalogueId);
            resource.setPayload(serviceBundleManager.serialize(serviceBundle));
            logger.debug("Migrating Service: {} of Catalogue: {} to Catalogue: {}", serviceBundle.getId(), catalogueId, newCatalogueId);
            resourceService.updateResource(resource);
            if (sendJMS) {
                jmsService.convertAndSendTopic(jmsTopic, serviceBundle);
            }
        }
    }

    private void changeTrainingResourceCatalogue(String providerId, String catalogueId, String newCatalogueId, Authentication authentication) {
        List<TrainingResourceBundle> trainingResourceBundles = trainingResourceManager.getResourceBundles(catalogueId, providerId, authentication).getResults();
        // Training Resources
        String jmsTopic = "training_resource.update";
        boolean sendJMS;
        for (TrainingResourceBundle trainingResourceBundle : trainingResourceBundles) {
            sendJMS = false;
            String oldResourceId = trainingResourceBundle.getId();
            if (trainingResourceBundle.getTrainingResource().getId().startsWith(catalogueId)) {
                // if Training Resource is Public, update its id
                sendJMS = true;
                String id = trainingResourceBundle.getId().replaceFirst(catalogueId, newCatalogueId);
                trainingResourceBundle.getTrainingResource().setId(id);
            }
            trainingResourceBundle.getTrainingResource().setCatalogueId(newCatalogueId);
            Resource resource = trainingResourceManager.getResource(oldResourceId, catalogueId);
            resource.setPayload(trainingResourceManager.serialize(trainingResourceBundle));
            logger.debug("Migrating Training Resource: {} of Catalogue: {} to Catalogue: {}", trainingResourceBundle.getId(), catalogueId, newCatalogueId);
            resourceService.updateResource(resource);
            if (sendJMS) {
                jmsService.convertAndSendTopic(jmsTopic, trainingResourceBundle);
            }
        }
    }

    private void changeInteroperabilityRecordCatalogue(String providerId, String catalogueId, String newCatalogueId, Authentication authentication) {
        List<InteroperabilityRecordBundle> interoperabilityRecordBundles = interoperabilityRecordManager.getInteroperabilityRecordBundles(catalogueId, providerId, authentication).getResults();
        String jmsTopic = "interoperability_record.update";
        boolean sendJMS;
        for (InteroperabilityRecordBundle interoperabilityRecordBundle : interoperabilityRecordBundles) {
            sendJMS = false;
            String oldResourceId = interoperabilityRecordBundle.getId();
            if (interoperabilityRecordBundle.getInteroperabilityRecord().getId().startsWith(catalogueId)) {
                // if Interoperability Record is Public, update its id
                sendJMS = true;
                String id = interoperabilityRecordBundle.getId().replaceFirst(catalogueId, newCatalogueId);
                interoperabilityRecordBundle.getInteroperabilityRecord().setId(id);
            }
            interoperabilityRecordBundle.getInteroperabilityRecord().setCatalogueId(newCatalogueId);
            Resource resource = interoperabilityRecordManager.getResource(oldResourceId, catalogueId);
            resource.setPayload(interoperabilityRecordManager.serialize(interoperabilityRecordBundle));
            logger.debug("Migrating Interoperability Record: {} of Catalogue: {} to Catalogue: {}", interoperabilityRecordBundle.getId(), catalogueId, newCatalogueId);
            resourceService.updateResource(resource);
            if (sendJMS) {
                jmsService.convertAndSendTopic(jmsTopic, interoperabilityRecordBundle);
            }
        }
    }

    public void updateRelatedToTheIdFieldsOfOtherResourcesOfThePortal(String oldResourceId, String newResourceId) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("published", false);
        List<ServiceBundle> allServices = serviceBundleManager.getAllForAdmin(ff, securityService.getAdminAccess()).getResults();
        List<TrainingResourceBundle> allTrainingResources = trainingResourceManager.getAllForAdmin(ff, securityService.getAdminAccess()).getResults();
        List<DatasourceBundle> allDatasourceBundles = datasourceManager.getAll(ff, securityService.getAdminAccess()).getResults();
        List<ResourceInteroperabilityRecordBundle> allResourceInteroperabilityRecords = resourceInteroperabilityRecordManager.getAll(ff, securityService.getAdminAccess()).getResults();
        List<HelpdeskBundle> allHelpdeskBundles = helpdeskManager.getAll(ff, securityService.getAdminAccess()).getResults();
        List<MonitoringBundle> allMonitoringBundles = monitoringManager.getAll(ff, securityService.getAdminAccess()).getResults();

        for (ServiceBundle serviceBundle : allServices) {
            boolean entered = false;
            if (serviceBundle.getService().getRequiredResources() != null && !serviceBundle.getService().getRequiredResources().isEmpty()
                    && serviceBundle.getService().getRequiredResources().contains(oldResourceId)) {
                serviceBundle.getService().getRequiredResources().remove(oldResourceId);
                serviceBundle.getService().getRequiredResources().add(newResourceId);
                entered = true;
            }
            if (serviceBundle.getService().getRelatedResources() != null && !serviceBundle.getService().getRelatedResources().isEmpty()
                    && serviceBundle.getService().getRelatedResources().contains(oldResourceId)) {
                serviceBundle.getService().getRelatedResources().remove(oldResourceId);
                serviceBundle.getService().getRelatedResources().add(newResourceId);
                entered = true;
            }
            if (entered) {
                Resource resource = serviceBundleManager.getResource(serviceBundle.getId(), serviceBundle.getService().getCatalogueId());
                resource.setPayload(serviceBundleManager.serialize(serviceBundle));
                resourceService.updateResource(resource);
                // update Public Service
                publicServiceManager.update(serviceBundle, securityService.getAdminAccess());
            }
        }

        for (TrainingResourceBundle trainingResourceBundle : allTrainingResources) {
            if (trainingResourceBundle.getTrainingResource().getEoscRelatedServices() != null && !trainingResourceBundle.getTrainingResource().getEoscRelatedServices().isEmpty()
                    && trainingResourceBundle.getTrainingResource().getEoscRelatedServices().contains(oldResourceId)) {
                trainingResourceBundle.getTrainingResource().getEoscRelatedServices().remove(oldResourceId);
                trainingResourceBundle.getTrainingResource().getEoscRelatedServices().add(newResourceId);
                Resource resource = trainingResourceManager.getResource(trainingResourceBundle.getId(), trainingResourceBundle.getTrainingResource().getCatalogueId());
                resource.setPayload(trainingResourceManager.serialize(trainingResourceBundle));
                resourceService.updateResource(resource);
                // update Public Training Resource
                TrainingResourceBundle updatedTrainingResourceBundle = trainingResourceManager.get(newResourceId, catalogueId);
                publicTrainingResourceManager.update(updatedTrainingResourceBundle, securityService.getAdminAccess());
            }
        }

        for (DatasourceBundle datasourceBundle : allDatasourceBundles) {
            if (datasourceBundle.getDatasource().getServiceId().equals(oldResourceId)) {
                datasourceBundle.getDatasource().setServiceId(newResourceId);
                Resource resource = datasourceManager.getResource(datasourceBundle.getId());
                resource.setPayload(datasourceManager.serialize(datasourceBundle));
                resourceService.updateResource(resource);
                jmsService.convertAndSendTopic("datasource.update", datasourceBundle);
            }
        }

        for (ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle : allResourceInteroperabilityRecords) {
            if (resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getResourceId().equals(oldResourceId)) {
                resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().setResourceId(newResourceId);
                Resource resource = resourceInteroperabilityRecordManager.getResource(resourceInteroperabilityRecordBundle.getId());
                resource.setPayload(resourceInteroperabilityRecordManager.serialize(resourceInteroperabilityRecordBundle));
                resourceService.updateResource(resource);
                // update Public Resource Interoperability Record
                publicResourceInteroperabilityRecordManager.update(resourceInteroperabilityRecordBundle, securityService.getAdminAccess());
            }
        }

        for (HelpdeskBundle helpdeskBundle : allHelpdeskBundles) {
            if (helpdeskBundle.getHelpdesk().getServiceId().equals(oldResourceId)) {
                helpdeskBundle.getHelpdesk().setServiceId(newResourceId);
                Resource resource = helpdeskManager.getResource(helpdeskBundle.getId());
                resource.setPayload(helpdeskManager.serialize(helpdeskBundle));
                resourceService.updateResource(resource);
                jmsService.convertAndSendTopic("helpdesk.update", helpdeskBundle);
            }
        }

        for (MonitoringBundle monitoringBundle : allMonitoringBundles) {
            if (monitoringBundle.getMonitoring().getServiceId().equals(oldResourceId)) {
                monitoringBundle.getMonitoring().setServiceId(newResourceId);
                Resource resource = monitoringManager.getResource(monitoringBundle.getId());
                resource.setPayload(monitoringManager.serialize(monitoringBundle));
                resourceService.updateResource(resource);
                jmsService.convertAndSendTopic("monitoring.update", monitoringBundle);
            }
        }
    }

}
