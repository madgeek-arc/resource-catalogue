package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Identifiers;
import eu.einfracentral.domain.InfraService;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.ResourceCRUDService;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@Service("publicResourceManager")
public class PublicResourceManager extends ResourceManager<InfraService> implements ResourceCRUDService<InfraService, Authentication> {

    private static final Logger logger = LogManager.getLogger(PublicResourceManager.class);
    private final JmsTemplate jmsTopicTemplate;
    private final SecurityService securityService;

    @Autowired
    public PublicResourceManager(JmsTemplate jmsTopicTemplate, SecurityService securityService) {
        super(InfraService.class);
        this.jmsTopicTemplate = jmsTopicTemplate;
        this.securityService = securityService;
    }

    @Override
    public String getResourceType() {
        return "infra_service";
    }

    @Override
    public Browsing<InfraService> getAll(FacetFilter facetFilter, Authentication authentication) {
        return super.getAll(facetFilter, authentication);
    }

    @Override
    public Browsing<InfraService> getMy(FacetFilter facetFilter, Authentication authentication) {
        if (authentication == null) {
            throw new UnauthorizedUserException("Please log in.");
        }

        List<InfraService> infraServiceList = new ArrayList<>();
        Browsing<InfraService> infraServiceBrowsing = super.getAll(facetFilter, authentication);
        for (InfraService infraService : infraServiceBrowsing.getResults()) {
            if (securityService.isServiceProviderAdmin(authentication, infraService.getId()) && infraService.getMetadata().isPublished()) {
                infraServiceList.add(infraService);
            }
        }
        return new Browsing<>(infraServiceBrowsing.getTotal(), infraServiceBrowsing.getFrom(),
                infraServiceBrowsing.getTo(), infraServiceList, infraServiceBrowsing.getFacets());
    }

    @Override
    public InfraService add(InfraService infraService, Authentication authentication) {
        String lowerLevelResourceId = infraService.getId();
        infraService.setIdentifiers(Identifiers.createIdentifier(infraService.getId()));
        infraService.setId(String.format("%s.%s", infraService.getService().getCatalogueId(), infraService.getId()));
        infraService.getMetadata().setPublished(true);
        InfraService ret;
        logger.info(String.format("Resource [%s] is being published with id [%s]", lowerLevelResourceId, infraService.getId()));
        ret = super.add(infraService, null);
        jmsTopicTemplate.convertAndSend("public_resource.create", infraService);
        return ret;
    }

    @Override
    public InfraService update(InfraService infraService, Authentication authentication) {
        InfraService published = super.get(String.format("%s.%s", infraService.getService().getCatalogueId(), infraService.getId()));
        InfraService ret = super.get(String.format("%s.%s", infraService.getService().getCatalogueId(), infraService.getId()));
        try {
            BeanUtils.copyProperties(ret, infraService);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.setMetadata(published.getMetadata());
        logger.info(String.format("Updating public Resource with id [%s]", ret.getId()));
        ret = super.update(ret, null);
        jmsTopicTemplate.convertAndSend("public_resource.update", ret);
        return ret;
    }

    @Override
    public void delete(InfraService infraService) {
        InfraService publicInfraService = get(String.format("%s.%s",infraService.getService().getCatalogueId(), infraService.getId()));
        logger.info(String.format("Deleting public Resource with id [%s]", publicInfraService.getId()));
        super.delete(publicInfraService);
        jmsTopicTemplate.convertAndSend("public_resource.delete", publicInfraService);
    }
}