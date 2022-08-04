package eu.einfracentral.registry.manager;

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
    private final ProviderManager providerService;
    private final ResourceService resourceService;
    private final JmsTemplate jmsTopicTemplate;

    @Autowired
    public MigrationManager(ServiceBundleManager serviceBundleManager,
                            ProviderManager providerService,
                            ResourceService resourceService,
                            JmsTemplate jmsTopicTemplate) {
        this.serviceBundleManager = serviceBundleManager;
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
        jmsTopicTemplate.convertAndSend("provider.update", providerBundle);

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
            jmsTopicTemplate.convertAndSend("public_provider.update", publicProviderBundle);
        } catch (RuntimeException e) {
            logger.error("Error migrating Public Provider", e);
        }

        // Update provider's resources' catalogue
        changeResourceCatalogue(providerId, catalogueId, newCatalogueId, authentication);

        return providerBundle;
    }

    private void changeResourceCatalogue(String providerId, String catalogueId, String newCatalogueId, Authentication authentication) {
        List<ServiceBundle> serviceBundles = serviceBundleManager.getResourceBundles(providerId, authentication);
        // Resources
        String jmsTopic = "resource.update";
        for (ServiceBundle serviceBundle : serviceBundles) {
            String oldResourceId = serviceBundle.getId();
            if (serviceBundle.getService().getId().startsWith(catalogueId)) {
                // if Resource is Public, update its id
                jmsTopic = "public_resource.update";
                String id = serviceBundle.getId().replaceFirst(catalogueId, newCatalogueId);
                serviceBundle.getService().setId(id);
            }
            serviceBundle.getService().setCatalogueId(newCatalogueId);
            Resource resource = serviceBundleManager.getResource(oldResourceId, catalogueId);
            resource.setPayload(serviceBundleManager.serialize(serviceBundle));
            logger.debug("Migrating Resource: {} of Catalogue: {} to Catalogue: {}", serviceBundle.getId(), catalogueId, newCatalogueId);
            resourceService.updateResource(resource);
            jmsTopicTemplate.convertAndSend(jmsTopic, serviceBundle);
        }
    }
}
