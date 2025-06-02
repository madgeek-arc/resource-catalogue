package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import gr.uoa.di.madgik.resourcecatalogue.domain.AdapterBundle;
import gr.uoa.di.madgik.resourcecatalogue.exceptions.CatalogueResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.manager.pids.PidIssuer;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetLabelService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service("publicAdapterManager")
public class PublicAdapterService extends ResourceCatalogueManager<AdapterBundle> implements ResourceCRUDService<AdapterBundle, Authentication> {
    private static final Logger logger = LoggerFactory.getLogger(PublicProviderService.class);
    private final JmsService jmsService;
    private final PidIssuer pidIssuer;
    private final FacetLabelService facetLabelService;

    public PublicAdapterService(JmsService jmsService,
                                PidIssuer pidIssuer,
                                FacetLabelService facetLabelService) {
        super(AdapterBundle.class);
        this.jmsService = jmsService;
        this.pidIssuer = pidIssuer;
        this.facetLabelService = facetLabelService;
    }

    @Override
    public String getResourceTypeName() {
        return "adapter";
    }

    @Override
    public Browsing<AdapterBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        Browsing<AdapterBundle> browsing = super.getAll(facetFilter, authentication);
        if (!browsing.getResults().isEmpty() && !browsing.getFacets().isEmpty()) {
            browsing.setFacets(facetLabelService.generateLabels(browsing.getFacets()));
        }
        return browsing;
    }

    @Override
    public AdapterBundle add(AdapterBundle adapterBundle, Authentication authentication) {
        String lowerLevelAdapterId = adapterBundle.getId();
        adapterBundle.setId(adapterBundle.getIdentifiers().getPid());
        adapterBundle.getMetadata().setPublished(true);

        //POST PID
        //TODO: enable when we have PID configuration properties for Beyond
        logger.info("PID POST disabled");
//        pidIssuer.postPID(adapterBundle.getId(), null);

        AdapterBundle ret;
        logger.info("Adapter '{}' is being published with id '{}'", lowerLevelAdapterId, adapterBundle.getId());
        ret = super.add(adapterBundle, null);
        jmsService.convertAndSendTopic("adapter.create", adapterBundle);
        return ret;
    }

    @Override
    public AdapterBundle update(AdapterBundle adapterBundle, Authentication authentication) {
        AdapterBundle published = super.get(adapterBundle.getIdentifiers().getPid(),
                adapterBundle.getAdapter().getCatalogueId(), true);
        AdapterBundle ret = super.get(adapterBundle.getIdentifiers().getPid(),
                adapterBundle.getAdapter().getCatalogueId(), true);
        try {
            BeanUtils.copyProperties(ret, adapterBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.info("Could not copy properties.");
        }

        ret.setIdentifiers(published.getIdentifiers());
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info("Updating public Adapter with id '{}'", ret.getId());
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("adapter.update", ret);
        return ret;
    }

    @Override
    public void delete(AdapterBundle adapterBundle) {
        try {
            AdapterBundle publicAdapterBundle = get(adapterBundle.getIdentifiers().getPid(),
                    adapterBundle.getAdapter().getCatalogueId(), true);
            logger.info("Deleting public Adapter with id '{}'", publicAdapterBundle.getId());
            super.delete(publicAdapterBundle);
            jmsService.convertAndSendTopic("adapter.delete", publicAdapterBundle);
        } catch (CatalogueResourceNotFoundException ignore) {
        }
    }

}
