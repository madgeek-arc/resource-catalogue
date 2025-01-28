package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import gr.uoa.di.madgik.resourcecatalogue.domain.AlternativeIdentifier;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.utils.PublicResourceUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service("publicTrainingResourceManager")
public class PublicTrainingResourceManager extends AbstractPublicResourceManager<TrainingResourceBundle> implements ResourceCRUDService<TrainingResourceBundle, Authentication> {

    private static final Logger logger = LoggerFactory.getLogger(PublicTrainingResourceManager.class);
    private final JmsService jmsService;
    private final SecurityService securityService;
    private ProviderResourcesCommonMethods commonMethods;
    private final FacetLabelService facetLabelService;
    private final PublicResourceUtils publicResourceUtils;

    public PublicTrainingResourceManager(JmsService jmsService, SecurityService securityService,
                                         ProviderResourcesCommonMethods commonMethods,
                                         FacetLabelService facetLabelService,
                                         PublicResourceUtils publicResourceUtils) {
        super(TrainingResourceBundle.class);
        this.jmsService = jmsService;
        this.securityService = securityService;
        this.commonMethods = commonMethods;
        this.facetLabelService = facetLabelService;
        this.publicResourceUtils = publicResourceUtils;
    }

    @Override
    public String getResourceTypeName() {
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
    public TrainingResourceBundle add(TrainingResourceBundle trainingResourceBundle, Authentication authentication) {
        String lowerLevelResourceId = trainingResourceBundle.getId();
        Identifiers.createOriginalId(trainingResourceBundle);
        trainingResourceBundle.setId(publicResourceUtils.createPublicResourceId(trainingResourceBundle.getTrainingResource().getId(),
                trainingResourceBundle.getTrainingResource().getCatalogueId()));
        commonMethods.restrictPrefixRepetitionOnPublicResources(trainingResourceBundle.getId(),
                trainingResourceBundle.getTrainingResource().getCatalogueId());

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
            //TODO: enable when we have PID configuration properties for Beyond
            logger.info("PID POST disabled");
//            commonMethods.postPID(pid);
        }
        TrainingResourceBundle ret;
        logger.info(String.format("Training Resource [%s] is being published with id [%s]", lowerLevelResourceId, trainingResourceBundle.getId()));
        ret = super.add(trainingResourceBundle, null);
        jmsService.convertAndSendTopic("training_resource.create", trainingResourceBundle);
        return ret;
    }

    @Override
    public TrainingResourceBundle update(TrainingResourceBundle trainingResourceBundle, Authentication authentication) {
        TrainingResourceBundle published = super.get(publicResourceUtils.createPublicResourceId(
                trainingResourceBundle.getTrainingResource().getId(),
                trainingResourceBundle.getTrainingResource().getCatalogueId()));
        TrainingResourceBundle ret = super.get(publicResourceUtils.createPublicResourceId(trainingResourceBundle.getTrainingResource().getId(),
                trainingResourceBundle.getTrainingResource().getCatalogueId()));
        try {
            BeanUtils.copyProperties(ret, trainingResourceBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        // sets public ids to resource organisation, resource providers and EOSC related services
        updateTrainingResourceIdsToPublic(ret);

        ret.getTrainingResource().setAlternativeIdentifiers(published.getTrainingResource().getAlternativeIdentifiers());
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
            TrainingResourceBundle publicTrainingResourceBundle = get(publicResourceUtils.createPublicResourceId(
                    trainingResourceBundle.getTrainingResource().getId(),
                    trainingResourceBundle.getTrainingResource().getCatalogueId()));
            logger.info(String.format("Deleting public Training Resource with id [%s]", publicTrainingResourceBundle.getId()));
            super.delete(publicTrainingResourceBundle);
            jmsService.convertAndSendTopic("training_resource.delete", publicTrainingResourceBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }
}
