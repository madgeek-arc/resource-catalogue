package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;

public abstract class AbstractPublicResourceManager<T extends Bundle<?>> extends ResourceCatalogueManager<T> implements PublicResourceService<T> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractPublicResourceManager.class);
    private final JmsService jmsService;
    private final PidIssuer pidIssuer;
    private final FacetLabelService facetLabelService;

    @Value("${pid.service.enabled}")
    private boolean pidServiceEnabled;

    protected AbstractPublicResourceManager(Class<T> typeParameterClass, JmsService jmsService,
                                         PidIssuer pidIssuer,
                                         FacetLabelService facetLabelService) {
        super(typeParameterClass);
        this.jmsService = jmsService;
        this.pidIssuer = pidIssuer;
        this.facetLabelService = facetLabelService;
    }

    public T getOrElseReturnNull(String id) {
        T datasourceBundle;
        try {
            datasourceBundle = get(id, true);
        } catch (CatalogueResourceNotFoundException e) {
            return null;
        }
        return datasourceBundle;
    }

    @Override
    public Browsing<T> getAll(FacetFilter facetFilter, Authentication authentication) {
        Browsing<T> browsing = super.getAll(facetFilter, authentication);
        if (!browsing.getResults().isEmpty() && !browsing.getFacets().isEmpty()) {
            browsing.setFacets(facetLabelService.generateLabels(browsing.getFacets()));
        }
        return browsing;
    }

    @Override
    public T add(T t, Authentication authentication) {
        String lowerLevelId = t.getId();
        t.setId(t.getIdentifiers().getPid());
        t.getMetadata().setPublished(true);

        // Post PID
        if (pidServiceEnabled) {
            logger.info("Posting {} with id {} to PID service", t.getClass().getSimpleName(), t.getId());
            pidIssuer.postPID(t.getId(), null);
        }

        // sets public ids to fields
        updateIdsToPublic(t);

        T ret;
        logger.info("{} '{}' is being published with id '{}'", t.getClass().getSimpleName(), lowerLevelId, t.getId());
        ret = super.add(t, null);
        jmsService.convertAndSendTopic("provider.create", t);
        return ret;
    }

    @Override
    public T update(T t, Authentication authentication) {
        T published = super.get(t.getIdentifiers().getPid(), true);
        t.setIdentifiers(published.getIdentifiers());
        t.setId(published.getId());
        t.getMetadata().setPublished(true);

        // sets public ids to fields
        updateIdsToPublic(t);

        logger.info("Updating public {} with id '{}'", t.getClass().getSimpleName(), t.getId());
        T ret = super.update(t, null);
        jmsService.convertAndSendTopic("provider.update", t);
        return ret;
    }

    @Override
    public void delete(T t) {
        try {
            T publicT = get(t.getIdentifiers().getPid(), true);
            logger.info("Deleting public {} with id '{}'", publicT.getClass().getSimpleName(), publicT.getId());
            super.delete(publicT);
            jmsService.convertAndSendTopic("provider.delete", publicT);
        } catch (CatalogueResourceNotFoundException ignore) {
        }
    }
}