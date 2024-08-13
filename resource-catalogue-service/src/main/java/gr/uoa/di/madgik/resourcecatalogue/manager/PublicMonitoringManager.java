package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.domain.MonitoringBundle;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@Service("publicMonitoringManager")
public class PublicMonitoringManager extends AbstractPublicResourceManager<MonitoringBundle> implements ResourceCRUDService<MonitoringBundle, Authentication> {

    private static final Logger logger = LoggerFactory.getLogger(PublicMonitoringManager.class);
    private final JmsService jmsService;
    private final SecurityService securityService;
    private final ProviderResourcesCommonMethods commonMethods;

    @Autowired
    public PublicMonitoringManager(JmsService jmsService, SecurityService securityService,
                                   ProviderResourcesCommonMethods commonMethods) {
        super(MonitoringBundle.class);
        this.jmsService = jmsService;
        this.securityService = securityService;
        this.commonMethods = commonMethods;
    }

    @Override
    public String getResourceType() {
        return "monitoring";
    }

    @Override
    public Browsing<MonitoringBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        return super.getAll(facetFilter, authentication);
    }

    @Override
    public Browsing<MonitoringBundle> getMy(FacetFilter facetFilter, Authentication authentication) {
        if (authentication == null) {
            throw new InsufficientAuthenticationException("Please log in.");
        }

        List<MonitoringBundle> monitoringBundleList = new ArrayList<>();
        Browsing<MonitoringBundle> monitoringBundleBrowsing = super.getAll(facetFilter, authentication);
        for (MonitoringBundle monitoringBundle : monitoringBundleBrowsing.getResults()) {
            if (securityService.isResourceProviderAdmin(authentication, monitoringBundle.getMonitoring().getServiceId(), monitoringBundle.getCatalogueId())
                    && monitoringBundle.getMetadata().isPublished()) {
                monitoringBundleList.add(monitoringBundle);
            }
        }
        return new Browsing<>(monitoringBundleBrowsing.getTotal(), monitoringBundleBrowsing.getFrom(),
                monitoringBundleBrowsing.getTo(), monitoringBundleList, monitoringBundleBrowsing.getFacets());
    }

    public MonitoringBundle getOrElseReturnNull(String id) {
        MonitoringBundle monitoringBundle;
        try {
            monitoringBundle = get(id);
        } catch (ResourceException | ResourceNotFoundException e) {
            return null;
        }
        return monitoringBundle;
    }

    @Override
    public MonitoringBundle add(MonitoringBundle monitoringBundle, Authentication authentication) {
        String lowerLevelResourceId = monitoringBundle.getId();
        Identifiers.createOriginalId(monitoringBundle);
        monitoringBundle.setId(String.format("%s.%s", monitoringBundle.getCatalogueId(), monitoringBundle.getId()));
        commonMethods.restrictPrefixRepetitionOnPublicResources(monitoringBundle.getId(), monitoringBundle.getCatalogueId());

        // sets public id to serviceId
        updateMonitoringIdsToPublic(monitoringBundle);

        monitoringBundle.getMetadata().setPublished(true);
        MonitoringBundle ret;
        logger.info(String.format("Monitoring [%s] is being published with id [%s]", lowerLevelResourceId, monitoringBundle.getId()));
        ret = super.add(monitoringBundle, null);
        jmsService.convertAndSendTopic("monitoring.create", monitoringBundle);
        return ret;
    }

    @Override
    public MonitoringBundle update(MonitoringBundle monitoringBundle, Authentication authentication) {
        MonitoringBundle published = super.get(String.format("%s.%s", monitoringBundle.getCatalogueId(), monitoringBundle.getId()));
        MonitoringBundle ret = super.get(String.format("%s.%s", monitoringBundle.getCatalogueId(), monitoringBundle.getId()));
        try {
            BeanUtils.copyProperties(ret, monitoringBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        // sets public id to serviceId
        updateMonitoringIdsToPublic(monitoringBundle);

        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info(String.format("Updating public Monitoring with id [%s]", ret.getId()));
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("monitoring.update", monitoringBundle);
        return ret;
    }

    @Override
    public void delete(MonitoringBundle monitoringBundle) {
        try {
            MonitoringBundle publicMonitoringBundle = get(String.format("%s.%s", monitoringBundle.getCatalogueId(), monitoringBundle.getId()));
            logger.info(String.format("Deleting public Monitoring with id [%s]", publicMonitoringBundle.getId()));
            super.delete(publicMonitoringBundle);
            jmsService.convertAndSendTopic("monitoring.delete", publicMonitoringBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }
}
