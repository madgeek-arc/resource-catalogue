package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.DatasourceBundle;
import eu.einfracentral.domain.ServiceBundle;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.domain.User;
import eu.einfracentral.registry.service.MigrationService;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ResourceService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MigrationManager implements MigrationService {

    private static final Logger logger = LogManager.getLogger(MigrationManager.class);

    private final ServiceBundleManager serviceBundleManager;
    private final DatasourceBundleManager datasourceBundleManager;
    private final ProviderManager providerService;
    private final ResourceService resourceService;
    private final JmsTemplate jmsTopicTemplate;

    @Autowired
    public MigrationManager(ServiceBundleManager serviceBundleManager,
                            DatasourceBundleManager datasourceBundleManager,
                            ProviderManager providerService,
                            ResourceService resourceService,
                            JmsTemplate jmsTopicTemplate) {
        this.serviceBundleManager = serviceBundleManager;
        this.datasourceBundleManager = datasourceBundleManager;
        this.providerService = providerService;
        this.resourceService = resourceService;
        this.jmsTopicTemplate = jmsTopicTemplate;
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

}
