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

@Service("publicDatasourceManager")
public class PublicDatasourceManager extends ResourceManager<DatasourceBundle> implements ResourceCRUDService<DatasourceBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(PublicServiceManager.class);
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
        return "service";
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
        datasourceBundle.getMetadata().setPublished(true);
        DatasourceBundle ret;
        logger.info(String.format("Resource [%s] is being published with id [%s]", lowerLevelResourceId, datasourceBundle.getId()));
        ret = super.add(datasourceBundle, null);
        jmsTopicTemplate.convertAndSend("public_resource.create", datasourceBundle);
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
        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.setMetadata(published.getMetadata());
        logger.info(String.format("Updating public Resource with id [%s]", ret.getId()));
        ret = super.update(ret, null);
        jmsTopicTemplate.convertAndSend("public_resource.update", ret);
        return ret;
    }

    @Override
    public void delete(DatasourceBundle datasourceBundle) {
        try{
            DatasourceBundle publicDatasourceBundle = get(String.format("%s.%s", datasourceBundle.getDatasource().getCatalogueId(), datasourceBundle.getId()));
            logger.info(String.format("Deleting public Resource with id [%s]", publicDatasourceBundle.getId()));
            super.delete(publicDatasourceBundle);
            jmsTopicTemplate.convertAndSend("public_resource.delete", publicDatasourceBundle);
        } catch (ResourceException | ResourceNotFoundException ignore){
        }
    }
}
