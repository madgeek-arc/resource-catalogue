package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@Service("publicProviderManager")
public class PublicProviderManager extends ResourceManager<ProviderBundle> implements ResourceCRUDService<ProviderBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(PublicProviderManager.class);
    private final JmsService jmsService;
    private final SecurityService securityService;
    private final ProviderResourcesCommonMethods commonMethods;
    @Autowired
    private FacetLabelService facetLabelService;

    @Autowired
    public PublicProviderManager(JmsService jmsService, SecurityService securityService,
                                 ProviderResourcesCommonMethods commonMethods) {
        super(ProviderBundle.class);
        this.jmsService = jmsService;
        this.securityService = securityService;
        this.commonMethods = commonMethods;
    }

    @Override
    public String getResourceType() {
        return "provider";
    }

    @Override
    public Browsing<ProviderBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        Browsing<ProviderBundle> browsing = super.getAll(facetFilter, authentication);
        if (!browsing.getResults().isEmpty() && !browsing.getFacets().isEmpty()) {
            browsing.setFacets(facetLabelService.createLabels(browsing.getFacets()));
        }
        return browsing;
    }

    @Override
    public Browsing<ProviderBundle> getMy(FacetFilter facetFilter, Authentication authentication) {
        if (authentication == null) {
            throw new UnauthorizedUserException("Please log in.");
        }

        List<ProviderBundle> providerList = new ArrayList<>();
        Browsing<ProviderBundle> providerBundleBrowsing = super.getAll(facetFilter, authentication);
        for (ProviderBundle providerBundle : providerBundleBrowsing.getResults()) {
            if (securityService.isProviderAdmin(authentication, providerBundle.getId(), providerBundle.getProvider().getCatalogueId()) && providerBundle.getMetadata().isPublished()) {
                providerList.add(providerBundle);
            }
        }
        return new Browsing<>(providerBundleBrowsing.getTotal(), providerBundleBrowsing.getFrom(),
                providerBundleBrowsing.getTo(), providerList, providerBundleBrowsing.getFacets());
    }

    @Override
    public ProviderBundle add(ProviderBundle providerBundle, Authentication authentication) {
        String lowerLevelProviderId = providerBundle.getId();
        Identifiers.createOriginalId(providerBundle);
        providerBundle.setId(String.format("%s.%s", providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
        commonMethods.restrictPrefixRepetitionOnPublicResources(providerBundle.getId(), providerBundle.getProvider().getCatalogueId());
        providerBundle.getMetadata().setPublished(true);
        // create PID and set it as Alternative Identifier
        commonMethods.createPIDAndCorrespondingAlternativeIdentifier(providerBundle, "providers/");
        ProviderBundle ret;
        logger.info(String.format("Provider [%s] is being published with id [%s]", lowerLevelProviderId, providerBundle.getId()));
        ret = super.add(providerBundle, null);
        jmsService.convertAndSendTopic("provider.create", providerBundle);
        return ret;
    }

    @Override
    public ProviderBundle update(ProviderBundle providerBundle, Authentication authentication) {
        ProviderBundle published = super.get(String.format("%s.%s", providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
        ProviderBundle ret = super.get(String.format("%s.%s", providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
        try {
            BeanUtils.copyProperties(ret, providerBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        ret.getProvider().setAlternativeIdentifiers(commonMethods.updateAlternativeIdentifiers(
                providerBundle.getProvider().getAlternativeIdentifiers(),
                published.getProvider().getAlternativeIdentifiers()));
        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info(String.format("Updating public Provider with id [%s]", ret.getId()));
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("provider.update", ret);
        return ret;
    }

    @Override
    public void delete(ProviderBundle providerBundle) {
        try {
            ProviderBundle publicProviderBundle = get(String.format("%s.%s", providerBundle.getProvider().getCatalogueId(), providerBundle.getId()));
            logger.info(String.format("Deleting public Provider with id [%s]", publicProviderBundle.getId()));
            super.delete(publicProviderBundle);
            jmsService.convertAndSendTopic("provider.delete", publicProviderBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }
}