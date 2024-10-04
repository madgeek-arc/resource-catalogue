package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import gr.uoa.di.madgik.resourcecatalogue.domain.AlternativeIdentifier;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.utils.PublicResourceUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@Service("publicProviderManager")
public class PublicProviderManager extends ResourceManager<ProviderBundle> implements ResourceCRUDService<ProviderBundle, Authentication> {

    private static final Logger logger = LoggerFactory.getLogger(PublicProviderManager.class);
    private final JmsService jmsService;
    private final SecurityService securityService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final FacetLabelService facetLabelService;
    private final PublicResourceUtils publicResourceUtils;

    public PublicProviderManager(JmsService jmsService, SecurityService securityService,
                                 ProviderResourcesCommonMethods commonMethods,
                                 FacetLabelService facetLabelService,
                                 PublicResourceUtils publicResourceUtils) {
        super(ProviderBundle.class);
        this.jmsService = jmsService;
        this.securityService = securityService;
        this.commonMethods = commonMethods;
        this.facetLabelService = facetLabelService;
        this.publicResourceUtils = publicResourceUtils;
    }

    @Override
    public String getResourceType() {
        return "provider";
    }

    @Override
    public Browsing<ProviderBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        Browsing<ProviderBundle> browsing = super.getAll(facetFilter, authentication);
        if (!browsing.getResults().isEmpty() && !browsing.getFacets().isEmpty()) {
            browsing.setFacets(facetLabelService.generateLabels(browsing.getFacets()));
        }
        return browsing;
    }

    @Override
    public Browsing<ProviderBundle> getMy(FacetFilter facetFilter, Authentication authentication) {
        if (authentication == null) {
            throw new InsufficientAuthenticationException("Please log in.");
        }

        List<ProviderBundle> providerList = new ArrayList<>();
        Browsing<ProviderBundle> providerBundleBrowsing = super.getAll(facetFilter, authentication);
        for (ProviderBundle providerBundle : providerBundleBrowsing.getResults()) {
            if (securityService.isProviderAdmin(authentication, providerBundle.getId(),
                    providerBundle.getProvider().getCatalogueId()) && providerBundle.getMetadata().isPublished()) {
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
        providerBundle.setId(publicResourceUtils.createPublicResourceId(providerBundle.getProvider().getId(),
                providerBundle.getProvider().getCatalogueId()));
        commonMethods.restrictPrefixRepetitionOnPublicResources(providerBundle.getId(), providerBundle.getProvider().getCatalogueId());
        providerBundle.getMetadata().setPublished(true);
        // POST PID
        String pid = "no_pid";
        for (AlternativeIdentifier alternativeIdentifier : providerBundle.getProvider().getAlternativeIdentifiers()) {
            if (alternativeIdentifier.getType().equalsIgnoreCase("EOSC PID")) {
                pid = alternativeIdentifier.getValue();
                break;
            }
        }
        if (pid.equalsIgnoreCase("no_pid")) {
            logger.info("Provider with id {} does not have a PID registered under its AlternativeIdentifiers.",
                    providerBundle.getId());
        } else {
            //TODO: enable when we have PID configuration properties for Beyond
            logger.info("PID POST disabled");
//            commonMethods.postPID(pid);
        }
        ProviderBundle ret;
        logger.info(String.format("Provider [%s] is being published with id [%s]", lowerLevelProviderId, providerBundle.getId()));
        ret = super.add(providerBundle, null);
        jmsService.convertAndSendTopic("provider.create", providerBundle);
        return ret;
    }

    @Override
    public ProviderBundle update(ProviderBundle providerBundle, Authentication authentication) {
        ProviderBundle published = super.get(publicResourceUtils.createPublicResourceId(providerBundle.getProvider().getId(),
                providerBundle.getProvider().getCatalogueId()));
        ProviderBundle ret = super.get(publicResourceUtils.createPublicResourceId(providerBundle.getProvider().getId(),
                providerBundle.getProvider().getCatalogueId()));
        try {
            BeanUtils.copyProperties(ret, providerBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        ret.getProvider().setAlternativeIdentifiers(published.getProvider().getAlternativeIdentifiers());
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
            ProviderBundle publicProviderBundle = get(publicResourceUtils.createPublicResourceId(
                    providerBundle.getProvider().getId(),
                    providerBundle.getProvider().getCatalogueId()));
            logger.info(String.format("Deleting public Provider with id [%s]", publicProviderBundle.getId()));
            super.delete(publicProviderBundle);
            jmsService.convertAndSendTopic("provider.delete", publicProviderBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }
}