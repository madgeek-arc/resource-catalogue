package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.NewBundle;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;

import java.lang.reflect.InvocationTargetException;

public abstract class AbstractPublicResourceManager<T extends NewBundle>
        implements gr.uoa.di.madgik.resourcecatalogue.service.PublicResourceService<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractPublicResourceManager.class);

    protected final GenericResourceService genericResourceService;
    private final JmsService jmsService;
    private final PidIssuer pidIssuer;
    private final FacetLabelService facetLabelService;

    @Value("${pid.service.enabled}")
    private boolean pidServiceEnabled;

    protected abstract String getResourceTypeName();

    //FIXME: Should PublicResourceService extend anything so here we override?
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

    public Browsing<T> getAll(FacetFilter facetFilter, Authentication authentication) {
        Browsing<T> browsing = genericResourceService.getResults(facetFilter);
        //TODO: test if we need this
//        if (!browsing.getResults().isEmpty() && !browsing.getFacets().isEmpty()) {
//            browsing.setFacets(facetLabelService.generateLabels(browsing.getFacets()));
//        }
        return browsing;
    }

    public T add(T t) {
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
        ret = genericResourceService.add(getResourceTypeName(), t);
        jmsService.convertAndSendTopic("provider.create", t);
        return ret;
    }

    public T update(T t) {
        T published = get(t.getIdentifiers().getPid(), t.getCatalogueId());
        t.setIdentifiers(published.getIdentifiers());
        t.setId(published.getId());
        t.getMetadata().setPublished(true);

        // sets public ids to fields
        updateIdsToPublic(t);

        logger.info("Updating public {} with id '{}'", t.getClass().getSimpleName(), t.getId());
        T ret;
        try {
            ret = genericResourceService.update(getResourceTypeName(), t.getId(), t);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        jmsService.convertAndSendTopic("provider.update", t);
        return ret;
    }

    public void delete(T t) {
        try {
            T published = get(t.getIdentifiers().getPid(), t.getCatalogueId());
            logger.info("Deleting public {} with id '{}'", published.getClass().getSimpleName(), published.getId());
            genericResourceService.delete(getResourceTypeName(), t.getId());
            jmsService.convertAndSendTopic("provider.delete", published);
        } catch (CatalogueResourceNotFoundException ignore) {
        }
    }

    @Override
    public T createPublicResource(T t, Authentication auth) {
        return add(t);
    }
}