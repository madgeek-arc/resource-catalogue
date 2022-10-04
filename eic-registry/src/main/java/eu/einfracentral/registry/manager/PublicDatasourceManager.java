package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.DatasourceBundle;
import eu.einfracentral.domain.Identifiers;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
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
import java.util.stream.Collectors;

@Service
public class PublicDatasourceManager extends ResourceManager<DatasourceBundle> implements ResourceCRUDService<DatasourceBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(PublicDatasourceManager.class);
    private final JmsTemplate jmsTopicTemplate;
    private final SecurityService securityService;

    @Autowired
    public PublicDatasourceManager(JmsTemplate jmsTopicTemplate, SecurityService securityService) {
        super(DatasourceBundle.class);
        this.jmsTopicTemplate = jmsTopicTemplate;
        this.securityService = securityService;
    }

    @Override
    public String getResourceType() {
        return "datasource";
    }

    @Override
    public Browsing<DatasourceBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        return super.getAll(facetFilter, authentication);
    }

    @Override
    public Browsing<DatasourceBundle> getMy(FacetFilter facetFilter, Authentication authentication) {
        if (authentication == null) {
            throw new UnauthorizedUserException("Please log in.");
        }

        List<DatasourceBundle> datasourceBundleList = new ArrayList<>();
        Browsing<DatasourceBundle> datasourceBundleBrowsing = super.getAll(facetFilter, authentication);
        for (DatasourceBundle datasourceBundle : datasourceBundleBrowsing.getResults()) {
            if (securityService.isResourceProviderAdmin(authentication, datasourceBundle.getId()) && datasourceBundle.getMetadata().isPublished()) {
                datasourceBundleList.add(datasourceBundle);
            }
        }
        return new Browsing<>(datasourceBundleBrowsing.getTotal(), datasourceBundleBrowsing.getFrom(),
                datasourceBundleBrowsing.getTo(), datasourceBundleList, datasourceBundleBrowsing.getFacets());
    }

    @Override
    public DatasourceBundle add(DatasourceBundle datasourceBundle, Authentication authentication) {
        String lowerLevelResourceId = datasourceBundle.getId();
        datasourceBundle.setIdentifiers(Identifiers.createIdentifier(datasourceBundle.getId()));
        datasourceBundle.setId(String.format("%s.%s", datasourceBundle.getDatasource().getCatalogueId(), datasourceBundle.getId()));
        datasourceBundle.getDatasource().setResourceOrganisation(String.format("%s.%s", datasourceBundle.getDatasource().getCatalogueId(), datasourceBundle.getDatasource().getResourceOrganisation()));
        // Resource Providers
        List<String> oldResourceProviders = datasourceBundle.getDatasource().getResourceProviders();
        List<String> newResourceProviders = new ArrayList<>();
        for (String resourceProvider : oldResourceProviders){
            if (!resourceProvider.contains(datasourceBundle.getDatasource().getCatalogueId())){
                resourceProvider = datasourceBundle.getDatasource().getCatalogueId() + "." + resourceProvider;
            }
            newResourceProviders.add(resourceProvider);
        }
        datasourceBundle.getDatasource().setResourceProviders(newResourceProviders.stream()
                .distinct() // remove duplicates
                .collect(Collectors.toList()));
        // Related Resources
        List<String> oldRelatedResources = datasourceBundle.getDatasource().getRelatedResources();
        List<String> newRelatedResources = new ArrayList<>();
        for (String relatedResource : oldRelatedResources){
            if (!relatedResource.contains(datasourceBundle.getDatasource().getCatalogueId())){
                relatedResource = datasourceBundle.getDatasource().getCatalogueId() + "." + relatedResource;
            }
            newRelatedResources.add(relatedResource);
        }
        datasourceBundle.getDatasource().setRelatedResources(newRelatedResources.stream()
                .distinct() // remove duplicates
                .collect(Collectors.toList()));
        // Required Resources
        List<String> oldRequiredResources = datasourceBundle.getDatasource().getRequiredResources();
        List<String> newRequiredResources = new ArrayList<>();
        for (String requiredResource : oldRequiredResources){
            if (!requiredResource.contains(datasourceBundle.getDatasource().getCatalogueId())){
                requiredResource = datasourceBundle.getDatasource().getCatalogueId() + "." + requiredResource;
            }
            newRequiredResources.add(requiredResource);
        }
        datasourceBundle.getDatasource().setRequiredResources(newRequiredResources.stream()
                .distinct() // remove duplicates
                .collect(Collectors.toList()));
        datasourceBundle.getMetadata().setPublished(true);
        DatasourceBundle ret;
        logger.info(String.format("Datasource [%s] is being published with id [%s]", lowerLevelResourceId, datasourceBundle.getId()));
        ret = super.add(datasourceBundle, null);
        logger.info("Sending JMS with topic 'datasource.create'");
        jmsTopicTemplate.convertAndSend("datasource.create", datasourceBundle);
        return ret;
    }

    @Override
    public DatasourceBundle update(DatasourceBundle datasourceBundle, Authentication authentication) {
        DatasourceBundle published = super.get(String.format("%s.%s", datasourceBundle.getDatasource().getCatalogueId(), datasourceBundle.getId()));
        DatasourceBundle ret = super.get(String.format("%s.%s", datasourceBundle.getDatasource().getCatalogueId(), datasourceBundle.getId()));
        try {
            BeanUtils.copyProperties(ret, datasourceBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        ret.getDatasource().setResourceOrganisation(ret.getDatasource().getCatalogueId() + "." + ret.getDatasource().getResourceOrganisation());
        // Resource Providers
        List<String> oldResourceProviders = datasourceBundle.getDatasource().getResourceProviders();
        List<String> newResourceProviders = new ArrayList<>();
        for (String resourceProvider : oldResourceProviders){
            if (!resourceProvider.contains(datasourceBundle.getDatasource().getCatalogueId())){
                resourceProvider = datasourceBundle.getDatasource().getCatalogueId() + "." + resourceProvider;
            }
            newResourceProviders.add(resourceProvider);
        }
        ret.getDatasource().setResourceProviders(newResourceProviders.stream()
                .distinct() // remove duplicates
                .collect(Collectors.toList()));
        // Related Resources
        List<String> oldRelatedResources = datasourceBundle.getDatasource().getRelatedResources();
        List<String> newRelatedResources = new ArrayList<>();
        for (String relatedResource : oldRelatedResources){
            if (!relatedResource.contains(datasourceBundle.getDatasource().getCatalogueId())){
                relatedResource = datasourceBundle.getDatasource().getCatalogueId() + "." + relatedResource;
            }
            newRelatedResources.add(relatedResource);
        }
        ret.getDatasource().setRelatedResources(newRelatedResources.stream()
                .distinct() // remove duplicates
                .collect(Collectors.toList()));
        // Required Resources
        List<String> oldRequiredResources = datasourceBundle.getDatasource().getRequiredResources();
        List<String> newRequiredResources = new ArrayList<>();
        for (String requiredResource : oldRequiredResources){
            if (!requiredResource.contains(datasourceBundle.getDatasource().getCatalogueId())){
                requiredResource = datasourceBundle.getDatasource().getCatalogueId() + "." + requiredResource;
            }
            newRequiredResources.add(requiredResource);
        }
        ret.getDatasource().setRequiredResources(newRequiredResources.stream()
                .distinct() // remove duplicates
                .collect(Collectors.toList()));
        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.setMetadata(published.getMetadata());
        logger.info(String.format("Updating public Datasource with id [%s]", ret.getId()));
        ret = super.update(ret, null);
        logger.info("Sending JMS with topic 'datasource.update'");
        jmsTopicTemplate.convertAndSend("datasource.update", ret);
        return ret;
    }

    @Override
    public void delete(DatasourceBundle datasourceBundle) {
        try{
            DatasourceBundle publicDatasourceBundle = get(String.format("%s.%s", datasourceBundle.getDatasource().getCatalogueId(), datasourceBundle.getId()));
            logger.info(String.format("Deleting public Datasource with id [%s]", publicDatasourceBundle.getId()));
            super.delete(publicDatasourceBundle);
            logger.info("Sending JMS with topic 'datasource.delete'");
            jmsTopicTemplate.convertAndSend("datasource.delete", publicDatasourceBundle);
        } catch (ResourceException | ResourceNotFoundException ignore){
        }
    }
}
