package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import gr.uoa.di.madgik.resourcecatalogue.domain.AlternativeIdentifier;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
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

@Service("publicServiceManager")
public class PublicServiceManager extends AbstractPublicResourceManager<ServiceBundle> implements ResourceCRUDService<ServiceBundle, Authentication> {

    private static final Logger logger = LoggerFactory.getLogger(PublicServiceManager.class);
    private final JmsService jmsService;
    private final SecurityService securityService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final FacetLabelService facetLabelService;

    @Autowired
    public PublicServiceManager(JmsService jmsService,
                                SecurityService securityService,
                                ProviderResourcesCommonMethods commonMethods,
                                FacetLabelService facetLabelService) {
        super(ServiceBundle.class);
        this.jmsService = jmsService;
        this.securityService = securityService;
        this.commonMethods = commonMethods;
        this.facetLabelService = facetLabelService;
    }

    @Override
    public String getResourceType() {
        return "service";
    }

    @Override
    public Browsing<ServiceBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        Browsing<ServiceBundle> browsing = super.getAll(facetFilter, authentication);
        if (!browsing.getResults().isEmpty() && !browsing.getFacets().isEmpty()) {
            browsing.setFacets(facetLabelService.generateLabels(browsing.getFacets()));
        }
        return browsing;
    }

    @Override
    public Browsing<ServiceBundle> getMy(FacetFilter facetFilter, Authentication authentication) {
        if (authentication == null) {
            throw new InsufficientAuthenticationException("Please log in.");
        }

        List<ServiceBundle> serviceBundleList = new ArrayList<>();
        Browsing<ServiceBundle> serviceBundleBrowsing = super.getAll(facetFilter, authentication);
        for (ServiceBundle serviceBundle : serviceBundleBrowsing.getResults()) {
            if (securityService.isResourceProviderAdmin(authentication, serviceBundle.getId(), serviceBundle.getService().getCatalogueId()) && serviceBundle.getMetadata().isPublished()) {
                serviceBundleList.add(serviceBundle);
            }
        }
        return new Browsing<>(serviceBundleBrowsing.getTotal(), serviceBundleBrowsing.getFrom(),
                serviceBundleBrowsing.getTo(), serviceBundleList, serviceBundleBrowsing.getFacets());
    }

    @Override
    public ServiceBundle add(ServiceBundle serviceBundle, Authentication authentication) {
        String lowerLevelResourceId = serviceBundle.getId();
        Identifiers.createOriginalId(serviceBundle);
        serviceBundle.setId(String.format("%s.%s", serviceBundle.getService().getCatalogueId(), serviceBundle.getId()));
        commonMethods.restrictPrefixRepetitionOnPublicResources(serviceBundle.getId(), serviceBundle.getService().getCatalogueId());

        // sets public ids to resource organisation, resource providers and related/required resources
        updateServiceIdsToPublic(serviceBundle);

        serviceBundle.getMetadata().setPublished(true);
        // POST PID
        String pid = "no_pid";
        for (AlternativeIdentifier alternativeIdentifier : serviceBundle.getService().getAlternativeIdentifiers()) {
            if (alternativeIdentifier.getType().equalsIgnoreCase("EOSC PID")) {
                pid = alternativeIdentifier.getValue();
                break;
            }
        }
        if (pid.equalsIgnoreCase("no_pid")) {
            logger.info("Service with id {} does not have a PID registered under its AlternativeIdentifiers.",
                    serviceBundle.getId());
        } else {
            commonMethods.postPID(pid);
        }
        ServiceBundle ret;
        logger.info(String.format("Service [%s] is being published with id [%s]", lowerLevelResourceId, serviceBundle.getId()));
        ret = super.add(serviceBundle, null);
        jmsService.convertAndSendTopic("service.create", serviceBundle);
        return ret;
    }

    @Override
    public ServiceBundle update(ServiceBundle serviceBundle, Authentication authentication) {
        ServiceBundle published = super.get(String.format("%s.%s", serviceBundle.getService().getCatalogueId(), serviceBundle.getId()));
        ServiceBundle ret = super.get(String.format("%s.%s", serviceBundle.getService().getCatalogueId(), serviceBundle.getId()));
        try {
            BeanUtils.copyProperties(ret, serviceBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        // sets public ids to resource organisation, resource providers and related/required resources
        updateServiceIdsToPublic(ret);

        ret.getService().setAlternativeIdentifiers(commonMethods.updateAlternativeIdentifiers(
                serviceBundle.getService().getAlternativeIdentifiers(),
                published.getService().getAlternativeIdentifiers()));
        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info(String.format("Updating public Service with id [%s]", ret.getId()));
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("service.update", ret);
        return ret;
    }

    @Override
    public void delete(ServiceBundle serviceBundle) {
        try {
            ServiceBundle publicServiceBundle = get(String.format("%s.%s", serviceBundle.getService().getCatalogueId(), serviceBundle.getId()));
            logger.info(String.format("Deleting public Service with id [%s]", publicServiceBundle.getId()));
            super.delete(publicServiceBundle);
            jmsService.convertAndSendTopic("service.delete", publicServiceBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }
}
