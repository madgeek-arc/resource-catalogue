package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.HighlightedResult;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;

import java.lang.reflect.InvocationTargetException;

public abstract class AbstractPublicResourceManager<T extends Bundle>
        implements gr.uoa.di.madgik.resourcecatalogue.service.PublicResourceService<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractPublicResourceManager.class);

    protected final GenericResourceService genericResourceService;
    private final JmsService jmsService;
    private final PidIssuer pidIssuer;
    private final FacetLabelService facetLabelService;

    @Value("${pid.service.enabled}")
    private boolean pidServiceEnabled;

    protected abstract String getResourceTypeName();

    protected AbstractPublicResourceManager(GenericResourceService genericResourceService,
                                            JmsService jmsService,
                                            PidIssuer pidIssuer,
                                            FacetLabelService facetLabelService) {
        this.genericResourceService = genericResourceService;
        this.jmsService = jmsService;
        this.pidIssuer = pidIssuer;
        this.facetLabelService = facetLabelService;
    }

    @Override
    public T get(String id) {
        return null;
    }

    @Override
    public T get(String id, String catalogueId) {
        if (catalogueId != null && !catalogueId.isBlank()) {
            return genericResourceService.get(getResourceTypeName(),
                    new SearchService.KeyValue("resource_internal_id", id),
                    new SearchService.KeyValue("catalogue_id", catalogueId),
                    new SearchService.KeyValue("published", "true"));
        }
        return genericResourceService.get(getResourceTypeName(),
                new SearchService.KeyValue("resource_internal_id", id),
                new SearchService.KeyValue("published", "true"));
    }

    public Paging<T> getAll(FacetFilter ff) {
        return getAll(ff, null);
    }

    public Paging<T> getAll(FacetFilter ff, Authentication authentication) {
        ff.setResourceType(getResourceTypeName());
        ff.addFilter("published", true);
        Paging<T> paging = genericResourceService.getResults(ff);
        //TODO: test if we need this
        if (!paging.getResults().isEmpty() && !paging.getFacets().isEmpty()) {
            paging.setFacets(facetLabelService.generateLabels(paging.getFacets()));
        }
        return paging;
    }

    @Override
    public Paging<HighlightedResult<T>> searchServices(FacetFilter ff) {
        ff.setResourceType(getResourceTypeName());
        ff.addFilter("published", true);
        Paging<HighlightedResult<T>> paging = genericResourceService.getHighlightedResults(ff);
        if (!paging.getResults().isEmpty() && !paging.getFacets().isEmpty()) {
            paging.setFacets(facetLabelService.generateLabels(paging.getFacets()));
        }
        return paging;
    }

    @Override
    public Paging<T> getMy(FacetFilter filter, Authentication authentication) {
        throw new NotImplementedException();
    }

    public T add(T t, Authentication auth) {
        return add(t, false);
    }

    public T add(T t, boolean registerPID) {
        String lowerLevelId = t.getId();
        t.setId(t.getIdentifiers().getPid());
        t.getMetadata().setPublished(true);

        // Post PID
        if (pidServiceEnabled && registerPID) {
            logger.info("Posting {} with id {} to PID service", t.getClass().getSimpleName(), t.getId());
            pidIssuer.postPID(t.getId(), null);
        }

        // sets public ids to fields
        updateIdsToPublic(t);

        T ret;
        logger.info("{} '{}' is being published with id '{}'", t.getClass().getSimpleName(), lowerLevelId, t.getId());
        ret = genericResourceService.add(getResourceTypeName(), t, false); //FIXME: issue with public CTI (Found more than one models)
        jmsService.convertAndSendTopic(getResourceTypeName() + ".create", t);
        return ret;
    }

    public T update(T t, Authentication authentication) {
        T published = get(t.getIdentifiers().getPid(), t.getCatalogueId());
        t.setIdentifiers(published.getIdentifiers());
        t.setId(published.getId());
        t.getMetadata().setPublished(true);

        // sets public ids to fields
        updateIdsToPublic(t);

        logger.info("Updating public {} with id '{}'", t.getClass().getSimpleName(), t.getId());
        T ret;
        ret = genericResourceService.update(getResourceTypeName(), t);
        jmsService.convertAndSendTopic(getResourceTypeName() + ".update", t);
        return ret;
    }

    public void delete(T t) {
        try {
            T published = get(t.getIdentifiers().getPid(), t.getCatalogueId());
            logger.info("Deleting public {} with id '{}'", published.getClass().getSimpleName(), published.getId());
            genericResourceService.delete(getResourceTypeName(), published.getId());
            jmsService.convertAndSendTopic(getResourceTypeName() + ".delete", published);
        } catch (CatalogueResourceNotFoundException ignore) {
        }
    }

    @Override
    public T createPublicResource(T t, Authentication auth) {
        return add(t, auth);
    }
}