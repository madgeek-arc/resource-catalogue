package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import gr.uoa.di.madgik.resourcecatalogue.domain.HelpdeskBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.utils.PublicResourceUtils;
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

@Service("publicHelpdeskManager")
public class PublicHelpdeskManager extends AbstractPublicResourceManager<HelpdeskBundle> implements ResourceCRUDService<HelpdeskBundle, Authentication> {

    private static final Logger logger = LoggerFactory.getLogger(PublicDatasourceManager.class);
    private final JmsService jmsService;
    private final SecurityService securityService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final PublicResourceUtils publicResourceUtils;

    @Autowired
    public PublicHelpdeskManager(JmsService jmsService, SecurityService securityService,
                                 ProviderResourcesCommonMethods commonMethods,
                                 PublicResourceUtils publicResourceUtils) {
        super(HelpdeskBundle.class);
        this.jmsService = jmsService;
        this.securityService = securityService;
        this.commonMethods = commonMethods;
        this.publicResourceUtils = publicResourceUtils;
    }

    @Override
    public String getResourceType() {
        return "helpdesk";
    }

    @Override
    public Browsing<HelpdeskBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        return super.getAll(facetFilter, authentication);
    }

    @Override
    public Browsing<HelpdeskBundle> getMy(FacetFilter facetFilter, Authentication authentication) {
        if (authentication == null) {
            throw new InsufficientAuthenticationException("Please log in.");
        }

        List<HelpdeskBundle> helpdeskBundleList = new ArrayList<>();
        Browsing<HelpdeskBundle> helpdeskBundleBrowsing = super.getAll(facetFilter, authentication);
        for (HelpdeskBundle helpdeskBundle : helpdeskBundleBrowsing.getResults()) {
            if (securityService.isResourceProviderAdmin(authentication, helpdeskBundle.getHelpdesk().getServiceId(), helpdeskBundle.getCatalogueId())
                    && helpdeskBundle.getMetadata().isPublished()) {
                helpdeskBundleList.add(helpdeskBundle);
            }
        }
        return new Browsing<>(helpdeskBundleBrowsing.getTotal(), helpdeskBundleBrowsing.getFrom(),
                helpdeskBundleBrowsing.getTo(), helpdeskBundleList, helpdeskBundleBrowsing.getFacets());
    }

    public HelpdeskBundle getOrElseReturnNull(String id) {
        HelpdeskBundle helpdeskBundle;
        try {
            helpdeskBundle = get(id);
        } catch (ResourceException | ResourceNotFoundException e) {
            return null;
        }
        return helpdeskBundle;
    }

    @Override
    public HelpdeskBundle add(HelpdeskBundle helpdeskBundle, Authentication authentication) {
        String lowerLevelResourceId = helpdeskBundle.getId();
        Identifiers.createOriginalId(helpdeskBundle);
        helpdeskBundle.setId(publicResourceUtils.createPublicResourceId(helpdeskBundle.getHelpdesk().getId(),
                helpdeskBundle.getCatalogueId()));
        commonMethods.restrictPrefixRepetitionOnPublicResources(helpdeskBundle.getId(), helpdeskBundle.getCatalogueId());

        // sets public id to serviceId
        updateHelpdeskIdsToPublic(helpdeskBundle);

        helpdeskBundle.getMetadata().setPublished(true);
        HelpdeskBundle ret;
        logger.info(String.format("Helpdesk [%s] is being published with id [%s]", lowerLevelResourceId, helpdeskBundle.getId()));
        ret = super.add(helpdeskBundle, null);
        jmsService.convertAndSendTopic("helpdesk.create", helpdeskBundle);
        return ret;
    }

    @Override
    public HelpdeskBundle update(HelpdeskBundle helpdeskBundle, Authentication authentication) {
        HelpdeskBundle published = super.get(publicResourceUtils.createPublicResourceId(helpdeskBundle.getHelpdesk().getId(),
                helpdeskBundle.getCatalogueId()));
        HelpdeskBundle ret = super.get(publicResourceUtils.createPublicResourceId(helpdeskBundle.getHelpdesk().getId(),
                helpdeskBundle.getCatalogueId()));
        try {
            BeanUtils.copyProperties(ret, helpdeskBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        // sets public id to serviceId
        updateHelpdeskIdsToPublic(helpdeskBundle);

        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info(String.format("Updating public Helpdesk with id [%s]", ret.getId()));
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("helpdesk.update", helpdeskBundle);
        return ret;
    }

    @Override
    public void delete(HelpdeskBundle helpdeskBundle) {
        try {
            HelpdeskBundle publicHelpdeskBundle = get(publicResourceUtils.createPublicResourceId(helpdeskBundle.getHelpdesk().getId(),
                    helpdeskBundle.getCatalogueId()));
            logger.info(String.format("Deleting public Helpdesk with id [%s]", publicHelpdeskBundle.getId()));
            super.delete(publicHelpdeskBundle);
            jmsService.convertAndSendTopic("helpdesk.delete", publicHelpdeskBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }
}
