package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.registry.service.MigrationService;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ResourceService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MigrationManager implements MigrationService {

    private static final Logger logger = LogManager.getLogger(MigrationManager.class);

    private final ServiceBundleManager serviceBundleManager;
    private final PublicServiceManager publicServiceManager;
    private final DatasourceBundleManager datasourceBundleManager;
    private final PublicDatasourceManager publicDatasourceManager;
    private final TrainingResourceManager trainingResourceManager;
    private final PublicTrainingResourceManager publicTrainingResourceManager;
    private final ProviderManager providerService;
    private final ResourceService resourceService;
    private final ResourceInteroperabilityRecordManager resourceInteroperabilityRecordManager;
    private final PublicResourceInteroperabilityManager publicResourceInteroperabilityManager;
    private final HelpdeskManager helpdeskManager;
    private final MonitoringManager monitoringManager;
    private final JmsTemplate jmsTopicTemplate;
    private final SecurityService securityService;

    @Value("${project.catalogue.name}")
    private String catalogueName;
    @Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    @Autowired
    public MigrationManager(ServiceBundleManager serviceBundleManager, PublicServiceManager publicServiceManager,
                            DatasourceBundleManager datasourceBundleManager, PublicDatasourceManager publicDatasourceManager,
                            TrainingResourceManager trainingResourceManager, PublicTrainingResourceManager publicTrainingResourceManager,
                            ProviderManager providerService, ResourceService resourceService,
                            ResourceInteroperabilityRecordManager resourceInteroperabilityRecordManager,
                            PublicResourceInteroperabilityManager publicResourceInteroperabilityManager,
                            HelpdeskManager helpdeskManager, MonitoringManager monitoringManager,
                            JmsTemplate jmsTopicTemplate, SecurityService securityService) {
        this.serviceBundleManager = serviceBundleManager;
        this.publicServiceManager = publicServiceManager;
        this.datasourceBundleManager = datasourceBundleManager;
        this.publicDatasourceManager = publicDatasourceManager;
        this.trainingResourceManager = trainingResourceManager;
        this.publicTrainingResourceManager = publicTrainingResourceManager;
        this.providerService = providerService;
        this.resourceService = resourceService;
        this.resourceInteroperabilityRecordManager = resourceInteroperabilityRecordManager;
        this.publicResourceInteroperabilityManager = publicResourceInteroperabilityManager;
        this.helpdeskManager = helpdeskManager;
        this.monitoringManager = monitoringManager;
        this.jmsTopicTemplate = jmsTopicTemplate;
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
            logger.info("Sending JMS with topic 'provider.update'");
            jmsTopicTemplate.convertAndSend("provider.update", publicProviderBundle);
        } catch (RuntimeException e) {
            logger.error("Error migrating Public Provider", e);
        }

        // Update provider's resources' catalogue
        changeServiceCatalogue(providerId, catalogueId, newCatalogueId, authentication);
        changeDatasourceCatalogue(providerId, catalogueId, newCatalogueId, authentication);
        changeTrainingResourceCatalogue(providerId, catalogueId, newCatalogueId, authentication);

        return providerBundle;
    }

    private void changeServiceCatalogue(String providerId, String catalogueId, String newCatalogueId, Authentication authentication) {
        List<ServiceBundle> serviceBundles = serviceBundleManager.getResourceBundles(providerId, authentication);
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
            if (sendJMS){
                logger.info("Sending JMS with topic 'service.update'");
                jmsTopicTemplate.convertAndSend(jmsTopic, serviceBundle);
            }
        }
    }

    private void changeDatasourceCatalogue(String providerId, String catalogueId, String newCatalogueId, Authentication authentication) {
        List<DatasourceBundle> datasourceBundles = datasourceBundleManager.getResourceBundles(providerId, authentication);
        // Datasources
        String jmsTopic = "datasource.update";
        boolean sendJMS;
        for (DatasourceBundle datasourceBundle : datasourceBundles) {
            sendJMS = false;
            String oldResourceId = datasourceBundle.getId();
            if (datasourceBundle.getDatasource().getId().startsWith(catalogueId)) {
                // if Datasource is Public, update its id
                sendJMS = true;
                String id = datasourceBundle.getId().replaceFirst(catalogueId, newCatalogueId);
                datasourceBundle.getDatasource().setId(id);
            }
            datasourceBundle.getDatasource().setCatalogueId(newCatalogueId);
            Resource resource = datasourceBundleManager.getResource(oldResourceId, catalogueId);
            resource.setPayload(datasourceBundleManager.serialize(datasourceBundle));
            logger.debug("Migrating Datasource: {} of Catalogue: {} to Catalogue: {}", datasourceBundle.getId(), catalogueId, newCatalogueId);
            resourceService.updateResource(resource);
            if (sendJMS){
                logger.info("Sending JMS with topic 'datasource.update'");
                jmsTopicTemplate.convertAndSend(jmsTopic, datasourceBundle);
            }
        }
    }

    private void changeTrainingResourceCatalogue(String providerId, String catalogueId, String newCatalogueId, Authentication authentication) {
        List<TrainingResourceBundle> trainingResourceBundles = trainingResourceManager.getResourceBundles(providerId, authentication);
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
            if (sendJMS){
                logger.info("Sending JMS with topic 'training_resource.update'");
                jmsTopicTemplate.convertAndSend(jmsTopic, trainingResourceBundle);
            }
        }
    }

    public void updateRelatedToTheIdFieldsOfOtherResourcesOfThePortal(String oldResourceId, String newResourceId) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("published", false);
        List<ServiceBundle> allServices = serviceBundleManager.getAllForAdmin(ff, securityService.getAdminAccess()).getResults();
        List<DatasourceBundle> allDatasources = datasourceBundleManager.getAllForAdmin(ff, securityService.getAdminAccess()).getResults();
        List<TrainingResourceBundle> allTrainingResources = trainingResourceManager.getAllForAdmin(ff, securityService.getAdminAccess()).getResults();
        List<ResourceInteroperabilityRecordBundle> allResourceInteroperabilityRecords = resourceInteroperabilityRecordManager.getAll(ff, securityService.getAdminAccess()).getResults();
        List<HelpdeskBundle> allHelpdeskBundles = helpdeskManager.getAll(ff, securityService.getAdminAccess()).getResults();
        List<MonitoringBundle> allMonitoringBundles = monitoringManager.getAll(ff, securityService.getAdminAccess()).getResults();

        for (ServiceBundle serviceBundle : allServices){
            boolean entered = false;
            if (serviceBundle.getService().getRequiredResources() != null && !serviceBundle.getService().getRequiredResources().isEmpty()
                    && serviceBundle.getService().getRequiredResources().contains(oldResourceId)){
                serviceBundle.getService().getRequiredResources().remove(oldResourceId);
                serviceBundle.getService().getRequiredResources().add(newResourceId);
                entered = true;
            }
            if (serviceBundle.getService().getRelatedResources() != null && !serviceBundle.getService().getRelatedResources().isEmpty()
                    && serviceBundle.getService().getRelatedResources().contains(oldResourceId)){
                serviceBundle.getService().getRelatedResources().remove(oldResourceId);
                serviceBundle.getService().getRelatedResources().add(newResourceId);
                entered = true;
            }
            if (entered){
                Resource resource = serviceBundleManager.getResource(serviceBundle.getId(), serviceBundle.getService().getCatalogueId());
                resource.setPayload(serviceBundleManager.serialize(serviceBundle));
                resourceService.updateResource(resource);
                // update Public Service
                publicServiceManager.update(serviceBundle, securityService.getAdminAccess());
            }
        }

        for (DatasourceBundle datasourceBundle : allDatasources){
            boolean entered = false;
            if (datasourceBundle.getDatasource().getRequiredResources() != null && !datasourceBundle.getDatasource().getRequiredResources().isEmpty()
                    && datasourceBundle.getDatasource().getRequiredResources().contains(oldResourceId)){
                datasourceBundle.getDatasource().getRequiredResources().remove(oldResourceId);
                datasourceBundle.getDatasource().getRequiredResources().add(newResourceId);
                entered = true;
            }
            if (datasourceBundle.getDatasource().getRelatedResources() != null && !datasourceBundle.getDatasource().getRelatedResources().isEmpty()
                    && datasourceBundle.getDatasource().getRelatedResources().contains(oldResourceId)){
                datasourceBundle.getDatasource().getRelatedResources().remove(oldResourceId);
                datasourceBundle.getDatasource().getRelatedResources().add(newResourceId);
                entered = true;
            }
            if (entered){
                Resource resource = datasourceBundleManager.getResource(datasourceBundle.getId(), datasourceBundle.getDatasource().getCatalogueId());
                resource.setPayload(datasourceBundleManager.serialize(datasourceBundle));
                resourceService.updateResource(resource);
                // update Public Datasource
                publicDatasourceManager.update(datasourceBundle, securityService.getAdminAccess());
            }
        }

        for (TrainingResourceBundle trainingResourceBundle : allTrainingResources){
            if (trainingResourceBundle.getTrainingResource().getEoscRelatedServices() != null && !trainingResourceBundle.getTrainingResource().getEoscRelatedServices().isEmpty()
                    && trainingResourceBundle.getTrainingResource().getEoscRelatedServices().contains(oldResourceId)){
                trainingResourceBundle.getTrainingResource().getEoscRelatedServices().remove(oldResourceId);
                trainingResourceBundle.getTrainingResource().getEoscRelatedServices().add(newResourceId);
                Resource resource = trainingResourceManager.getResource(trainingResourceBundle.getId(), trainingResourceBundle.getTrainingResource().getCatalogueId());
                resource.setPayload(trainingResourceManager.serialize(trainingResourceBundle));
                resourceService.updateResource(resource);
                // update Public Training Resource
                TrainingResourceBundle updatedTrainingResourceBundle = trainingResourceManager.get(newResourceId, catalogueName);
                publicTrainingResourceManager.update(updatedTrainingResourceBundle, securityService.getAdminAccess());
            }
        }

        for (ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle : allResourceInteroperabilityRecords){
            if (resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().getResourceId().equals(oldResourceId)){
                resourceInteroperabilityRecordBundle.getResourceInteroperabilityRecord().setResourceId(newResourceId);
                Resource resource = resourceInteroperabilityRecordManager.getResource(resourceInteroperabilityRecordBundle.getId());
                resource.setPayload(resourceInteroperabilityRecordManager.serialize(resourceInteroperabilityRecordBundle));
                resourceService.updateResource(resource);
                // update Public Resource Interoperability Record
                publicResourceInteroperabilityManager.update(resourceInteroperabilityRecordBundle, securityService.getAdminAccess());
            }
        }

        for (HelpdeskBundle helpdeskBundle : allHelpdeskBundles){
            if (helpdeskBundle.getHelpdesk().getServiceId().equals(oldResourceId)){
                helpdeskBundle.getHelpdesk().setServiceId(newResourceId);
                Resource resource = helpdeskManager.getResource(helpdeskBundle.getId());
                resource.setPayload(helpdeskManager.serialize(helpdeskBundle));
                resourceService.updateResource(resource);
                jmsTopicTemplate.convertAndSend("helpdesk.update", helpdeskBundle);
            }
        }

        for (MonitoringBundle monitoringBundle : allMonitoringBundles){
            if (monitoringBundle.getMonitoring().getServiceId().equals(oldResourceId)){
                monitoringBundle.getMonitoring().setServiceId(newResourceId);
                Resource resource = monitoringManager.getResource(monitoringBundle.getId());
                resource.setPayload(monitoringManager.serialize(monitoringBundle));
                resourceService.updateResource(resource);
                jmsTopicTemplate.convertAndSend("monitoring.update", monitoringBundle);
            }
        }
    }

}
