package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.utils.PublicResourceUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service("publicDatasourceManager")
public class PublicDatasourceManager extends AbstractPublicResourceManager<DatasourceBundle> implements ResourceCRUDService<DatasourceBundle, Authentication> {

    private static final Logger logger = LoggerFactory.getLogger(PublicDatasourceManager.class);
    private final JmsService jmsService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final PublicResourceUtils publicResourceUtils;

    public PublicDatasourceManager(JmsService jmsService,
                                   ProviderResourcesCommonMethods commonMethods,
                                   PublicResourceUtils publicResourceUtils) {
        super(DatasourceBundle.class);
        this.jmsService = jmsService;
        this.commonMethods = commonMethods;
        this.publicResourceUtils = publicResourceUtils;
    }

    @Override
    public String getResourceTypeName() {
        return "datasource";
    }

    @Override
    public Browsing<DatasourceBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        return super.getAll(facetFilter, authentication);
    }

    public DatasourceBundle getOrElseReturnNull(String id) {
        DatasourceBundle datasourceBundle;
        try {
            datasourceBundle = get(id);
        } catch (ResourceException | ResourceNotFoundException e) {
            return null;
        }
        return datasourceBundle;
    }

    @Override
    public DatasourceBundle add(DatasourceBundle datasourceBundle, Authentication authentication) {
        String lowerLevelResourceId = datasourceBundle.getId();
        Identifiers.createOriginalId(datasourceBundle);
        datasourceBundle.setId(publicResourceUtils.createPublicResourceId(datasourceBundle.getDatasource().getId(),
                datasourceBundle.getDatasource().getCatalogueId()));
        commonMethods.restrictPrefixRepetitionOnPublicResources(datasourceBundle.getId(), datasourceBundle.getDatasource().getCatalogueId());

        // sets public ids to providerId, serviceId
        updateDatasourceIdsToPublic(datasourceBundle);

        datasourceBundle.getMetadata().setPublished(true);
        DatasourceBundle ret;
        logger.info("Datasource '{}' is being published with id '{}'", lowerLevelResourceId, datasourceBundle.getId());
        ret = super.add(datasourceBundle, null);
        jmsService.convertAndSendTopic("datasource.create", datasourceBundle);
        return ret;
    }

    @Override
    public DatasourceBundle update(DatasourceBundle datasourceBundle, Authentication authentication) {
        DatasourceBundle published = super.get(publicResourceUtils.createPublicResourceId(datasourceBundle.getDatasource().getId(),
                datasourceBundle.getDatasource().getCatalogueId()));
        DatasourceBundle ret = super.get(publicResourceUtils.createPublicResourceId(datasourceBundle.getDatasource().getId(),
                datasourceBundle.getDatasource().getCatalogueId()));
        try {
            BeanUtils.copyProperties(ret, datasourceBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.info("Could not copy properties.");
        }

        // sets public ids to providerId, serviceId
        updateDatasourceIdsToPublic(datasourceBundle);

        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info("Updating public Datasource with id '{}'", ret.getId());
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("datasource.update", ret);
        return ret;
    }

    @Override
    public void delete(DatasourceBundle datasourceBundle) {
        try {
            DatasourceBundle publicDatasourceBundle = get(publicResourceUtils.createPublicResourceId(
                    datasourceBundle.getDatasource().getId(),
                    datasourceBundle.getDatasource().getCatalogueId()));
            logger.info("Deleting public Datasource with id '{}'", publicDatasourceBundle.getId());
            super.delete(publicDatasourceBundle);
            jmsService.convertAndSendTopic("datasource.delete", publicDatasourceBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }
}
