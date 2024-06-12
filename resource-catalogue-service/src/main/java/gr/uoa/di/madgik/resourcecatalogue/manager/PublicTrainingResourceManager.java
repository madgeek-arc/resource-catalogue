package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import gr.uoa.di.madgik.resourcecatalogue.domain.AlternativeIdentifier;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
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

@Service("publicTrainingResourceManager")
public class PublicTrainingResourceManager extends AbstractPublicResourceManager<TrainingResourceBundle> implements ResourceCRUDService<TrainingResourceBundle, Authentication> {

    private static final Logger logger = LoggerFactory.getLogger(PublicTrainingResourceManager.class);
    private final JmsService jmsService;
    private final SecurityService securityService;
    private ProviderResourcesCommonMethods commonMethods;
    private final FacetLabelService facetLabelService;

    @Autowired
    public PublicTrainingResourceManager(JmsService jmsService, SecurityService securityService,
                                         ProviderResourcesCommonMethods commonMethods,
                                         FacetLabelService facetLabelService) {
        super(TrainingResourceBundle.class);
        this.jmsService = jmsService;
        this.securityService = securityService;
        this.commonMethods = commonMethods;
        this.facetLabelService = facetLabelService;
    }

    @Override
    public String getResourceType() {
        return "training_resource";
    }

    @Override
    public Browsing<TrainingResourceBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        Browsing<TrainingResourceBundle> browsing = super.getAll(facetFilter, authentication);
        if (!browsing.getResults().isEmpty() && !browsing.getFacets().isEmpty()) {
            browsing.setFacets(facetLabelService.generateLabels(browsing.getFacets()));
        }
        return browsing;
    }

    @Override
    public Browsing<TrainingResourceBundle> getMy(FacetFilter facetFilter, Authentication authentication) {
        if (authentication == null) {
            throw new InsufficientAuthenticationException("Please log in.");
        }

        List<TrainingResourceBundle> trainingResourceBundleList = new ArrayList<>();
        Browsing<TrainingResourceBundle> trainingResourceBundleBrowsing = super.getAll(facetFilter, authentication);
        for (TrainingResourceBundle trainingResourceBundle : trainingResourceBundleBrowsing.getResults()) {
            if (securityService.isResourceProviderAdmin(authentication, trainingResourceBundle.getId(),
                    trainingResourceBundle.getTrainingResource().getCatalogueId()) && trainingResourceBundle.getMetadata().isPublished()) {
                trainingResourceBundleList.add(trainingResourceBundle);
            }
        }
        return new Browsing<>(trainingResourceBundleBrowsing.getTotal(), trainingResourceBundleBrowsing.getFrom(),
                trainingResourceBundleBrowsing.getTo(), trainingResourceBundleList, trainingResourceBundleBrowsing.getFacets());
    }

    @Override
    public TrainingResourceBundle add(TrainingResourceBundle trainingResourceBundle, Authentication authentication) {
        String lowerLevelResourceId = trainingResourceBundle.getId();
        Identifiers.createOriginalId(trainingResourceBundle);
        trainingResourceBundle.setId(String.format("%s.%s", trainingResourceBundle.getTrainingResource().getCatalogueId(), trainingResourceBundle.getId()));
        commonMethods.restrictPrefixRepetitionOnPublicResources(trainingResourceBundle.getId(), trainingResourceBundle.getTrainingResource().getCatalogueId());

        // sets public ids to resource organisation, resource providers and EOSC related services
        updateTrainingResourceIdsToPublic(trainingResourceBundle);

        trainingResourceBundle.getMetadata().setPublished(true);
        // POST PID
        String pid = "no_pid";
        for (AlternativeIdentifier alternativeIdentifier : trainingResourceBundle.getTrainingResource().getAlternativeIdentifiers()) {
            if (alternativeIdentifier.getType().equalsIgnoreCase("EOSC PID")) {
                pid = alternativeIdentifier.getValue();
                break;
            }
        }
        if (pid.equalsIgnoreCase("no_pid")) {
            logger.info("Training Resource with id {} does not have a PID registered under its AlternativeIdentifiers.",
                    trainingResourceBundle.getId());
        } else {
            commonMethods.postPID(pid);
        }
        TrainingResourceBundle ret;
        logger.info(String.format("Training Resource [%s] is being published with id [%s]", lowerLevelResourceId, trainingResourceBundle.getId()));
        ret = super.add(trainingResourceBundle, null);
        jmsService.convertAndSendTopic("training_resource.create", trainingResourceBundle);
        return ret;
    }

    @Override
    public TrainingResourceBundle update(TrainingResourceBundle trainingResourceBundle, Authentication authentication) {
        TrainingResourceBundle published = super.get(String.format("%s.%s", trainingResourceBundle.getTrainingResource().getCatalogueId(), trainingResourceBundle.getId()));
        TrainingResourceBundle ret = super.get(String.format("%s.%s", trainingResourceBundle.getTrainingResource().getCatalogueId(), trainingResourceBundle.getId()));
        try {
            BeanUtils.copyProperties(ret, trainingResourceBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        // sets public ids to resource organisation, resource providers and EOSC related services
        updateTrainingResourceIdsToPublic(ret);

        ret.getTrainingResource().setAlternativeIdentifiers(commonMethods.updateAlternativeIdentifiers(
                trainingResourceBundle.getTrainingResource().getAlternativeIdentifiers(),
                published.getTrainingResource().getAlternativeIdentifiers()));
        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info(String.format("Updating public Training Resource with id [%s]", ret.getId()));
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("training_resource.update", ret);
        return ret;
    }

    @Override
    public void delete(TrainingResourceBundle trainingResourceBundle) {
        try {
            TrainingResourceBundle publicTrainingResourceBundle = get(String.format("%s.%s", trainingResourceBundle.getTrainingResource().getCatalogueId(), trainingResourceBundle.getId()));
            logger.info(String.format("Deleting public Training Resource with id [%s]", publicTrainingResourceBundle.getId()));
            super.delete(publicTrainingResourceBundle);
            jmsService.convertAndSendTopic("training_resource.delete", publicTrainingResourceBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }
}
