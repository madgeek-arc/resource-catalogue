package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import gr.uoa.di.madgik.resourcecatalogue.domain.AlternativeIdentifier;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
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

@Service("publicInteroperabilityRecordManager")
public class PublicInteroperabilityRecordManager extends AbstractPublicResourceManager<InteroperabilityRecordBundle>
        implements ResourceCRUDService<InteroperabilityRecordBundle, Authentication> {

    private static final Logger logger = LoggerFactory.getLogger(PublicInteroperabilityRecordManager.class);
    private final JmsService jmsService;
    private final SecurityService securityService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final PublicResourceUtils publicResourceUtils;

    @Autowired
    public PublicInteroperabilityRecordManager(JmsService jmsService, SecurityService securityService,
                                               ProviderResourcesCommonMethods commonMethods,
                                               PublicResourceUtils publicResourceUtils) {
        super(InteroperabilityRecordBundle.class);
        this.jmsService = jmsService;
        this.securityService = securityService;
        this.commonMethods = commonMethods;
        this.publicResourceUtils = publicResourceUtils;
    }

    @Override
    public String getResourceType() {
        return "interoperability_record";
    }

    @Override
    public Browsing<InteroperabilityRecordBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        return super.getAll(facetFilter, authentication);
    }

    @Override
    public Browsing<InteroperabilityRecordBundle> getMy(FacetFilter facetFilter, Authentication authentication) {
        if (authentication == null) {
            throw new InsufficientAuthenticationException("Please log in.");
        }

        List<InteroperabilityRecordBundle> interoperabilityRecordBundleList = new ArrayList<>();
        Browsing<InteroperabilityRecordBundle> interoperabilityRecordBundleBrowsing = super.getAll(facetFilter, authentication);
        for (InteroperabilityRecordBundle interoperabilityRecordBundle : interoperabilityRecordBundleBrowsing.getResults()) {
            if (securityService.isResourceProviderAdmin(authentication, interoperabilityRecordBundle.getId(),
                    interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId())
                    && interoperabilityRecordBundle.getMetadata().isPublished()) {
                interoperabilityRecordBundleList.add(interoperabilityRecordBundle);
            }
        }
        return new Browsing<>(interoperabilityRecordBundleBrowsing.getTotal(), interoperabilityRecordBundleBrowsing.getFrom(),
                interoperabilityRecordBundleBrowsing.getTo(), interoperabilityRecordBundleList, interoperabilityRecordBundleBrowsing.getFacets());
    }

    @Override
    public InteroperabilityRecordBundle add(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication authentication) {
        String lowerLevelResourceId = interoperabilityRecordBundle.getId();
        Identifiers.createOriginalId(interoperabilityRecordBundle);
        interoperabilityRecordBundle.setId(publicResourceUtils.createPublicResourceId(
                interoperabilityRecordBundle.getInteroperabilityRecord().getId(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId()));
        commonMethods.restrictPrefixRepetitionOnPublicResources(interoperabilityRecordBundle.getId(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId());

        // sets public id to providerId
        updateInteroperabilityRecordIdsToPublic(interoperabilityRecordBundle);

        interoperabilityRecordBundle.getMetadata().setPublished(true);
        // POST PID
        String pid = "no_pid";
        for (AlternativeIdentifier alternativeIdentifier : interoperabilityRecordBundle.getInteroperabilityRecord().getAlternativeIdentifiers()) {
            if (alternativeIdentifier.getType().equalsIgnoreCase("EOSC PID")) {
                pid = alternativeIdentifier.getValue();
                break;
            }
        }
        if (pid.equalsIgnoreCase("no_pid")) {
            logger.info("Interoperability Record with id {} does not have a PID registered under its AlternativeIdentifiers.",
                    interoperabilityRecordBundle.getId());
        } else {
            //TODO: enable when we have PID configuration properties for Beyond
            logger.info("PID POST disabled");
//            commonMethods.postPID(pid);
        }
        InteroperabilityRecordBundle ret;
        logger.info(String.format("Interoperability Record [%s] is being published with id [%s]", lowerLevelResourceId,
                interoperabilityRecordBundle.getId()));
        ret = super.add(interoperabilityRecordBundle, null);
        jmsService.convertAndSendTopic("interoperability_record.create", interoperabilityRecordBundle);
        return ret;
    }

    @Override
    public InteroperabilityRecordBundle update(InteroperabilityRecordBundle interoperabilityRecordBundle, Authentication authentication) {
        InteroperabilityRecordBundle published = super.get(publicResourceUtils.createPublicResourceId(
                interoperabilityRecordBundle.getInteroperabilityRecord().getId(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId()));
        InteroperabilityRecordBundle ret = super.get(publicResourceUtils.createPublicResourceId(
                interoperabilityRecordBundle.getInteroperabilityRecord().getId(),
                interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId()));
        try {
            BeanUtils.copyProperties(ret, interoperabilityRecordBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        // sets public id to providerId
        updateInteroperabilityRecordIdsToPublic(interoperabilityRecordBundle);

        ret.getInteroperabilityRecord().setAlternativeIdentifiers(published.getInteroperabilityRecord().getAlternativeIdentifiers());
        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info(String.format("Updating public Interoperability Record with id [%s]", ret.getId()));
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("interoperability_record.update", ret);
        return ret;
    }

    @Override
    public void delete(InteroperabilityRecordBundle interoperabilityRecordBundle) {
        try {
            InteroperabilityRecordBundle publicInteroperabilityRecordBundle = get(publicResourceUtils.createPublicResourceId(
                    interoperabilityRecordBundle.getInteroperabilityRecord().getId(),
                    interoperabilityRecordBundle.getInteroperabilityRecord().getCatalogueId()));
            logger.info(String.format("Deleting public Interoperability Record with id [%s]", publicInteroperabilityRecordBundle.getId()));
            super.delete(publicInteroperabilityRecordBundle);
            jmsService.convertAndSendTopic("interoperability_record.delete", publicInteroperabilityRecordBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

}
