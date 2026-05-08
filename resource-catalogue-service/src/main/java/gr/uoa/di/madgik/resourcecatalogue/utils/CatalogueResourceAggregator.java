package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.CatalogueResources;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CatalogueResourceAggregator {

    private final OrganisationService organisationService;
    private final ServiceService serviceService;
    private final CatalogueService catalogueService;
    private final DatasourceService datasourceService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final AdapterService adapterService;
    private final DeployableApplicationService deployableApplicationService;

    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    public CatalogueResourceAggregator(@Lazy OrganisationService organisationService,
                                       @Lazy ServiceService serviceService,
                                       @Lazy CatalogueService catalogueService,
                                       @Lazy DatasourceService datasourceService,
                                       @Lazy TrainingResourceService trainingResourceService,
                                       @Lazy InteroperabilityRecordService interoperabilityRecordService,
                                       @Lazy AdapterService adapterService,
                                       @Lazy DeployableApplicationService deployableApplicationService) {
        this.organisationService = organisationService;
        this.serviceService = serviceService;
        this.catalogueService = catalogueService;
        this.datasourceService = datasourceService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.adapterService = adapterService;
        this.deployableApplicationService = deployableApplicationService;
    }

    public void deleteAllRelatedResources(OrganisationBundle bundle, Authentication auth) {
        String providerId = bundle.getId();
        String catalogueId = bundle.getCatalogueId();
        serviceService.getAllEOSCResourcesOfAProvider(providerId, createFacetFilter(catalogueId), auth)
                .getResults()
                .forEach(serviceService::delete);
        catalogueService.getAllEOSCResourcesOfAProvider(providerId, createFacetFilter(catalogueId), auth)
                .getResults()
                .forEach(catalogueService::delete);
        datasourceService.getAllEOSCResourcesOfAProvider(providerId, createFacetFilter(catalogueId), auth)
                .getResults()
                .forEach(datasourceService::delete);
        trainingResourceService.getAllEOSCResourcesOfAProvider(providerId, createFacetFilter(catalogueId), auth)
                .getResults()
                .forEach(trainingResourceService::delete);
        interoperabilityRecordService.getAllEOSCResourcesOfAProvider(providerId, createFacetFilter(catalogueId), auth)
                .getResults()
                .forEach(interoperabilityRecordService::delete);
        adapterService.getAllEOSCResourcesOfAProvider(providerId, createFacetFilter(catalogueId), auth)
                .getResults()
                .forEach(adapterService::delete);
        deployableApplicationService.getAllEOSCResourcesOfAProvider(providerId, createFacetFilter(catalogueId), auth)
                .getResults()
                .forEach(deployableApplicationService::delete);
    }

    public void suspendAllRelatedResources(OrganisationBundle bundle, Authentication auth) {
        String providerId = bundle.getId();
        String catalogueId = bundle.getCatalogueId();
        boolean suspended = bundle.isSuspended();
        serviceService.getAllEOSCResourcesOfAProvider(providerId, createFacetFilter(catalogueId), auth)
                .getResults()
                .forEach(s ->
                        serviceService.setSuspend(s.getId(), catalogueId, suspended, auth));
        catalogueService.getAllEOSCResourcesOfAProvider(providerId, createFacetFilter(catalogueId), auth)
                .getResults()
                .forEach(s ->
                        catalogueService.setSuspend(s.getId(), catalogueId, suspended, auth));
        datasourceService.getAllEOSCResourcesOfAProvider(providerId, createFacetFilter(catalogueId), auth)
                .getResults()
                .forEach(ds ->
                        datasourceService.setSuspend(ds.getId(), catalogueId, suspended, auth));
        trainingResourceService.getAllEOSCResourcesOfAProvider(providerId, createFacetFilter(catalogueId), auth)
                .getResults()
                .forEach(tr ->
                        trainingResourceService.setSuspend(tr.getId(), catalogueId, suspended, auth));
        interoperabilityRecordService.getAllEOSCResourcesOfAProvider(providerId, createFacetFilter(catalogueId), auth)
                .getResults()
                .forEach(ig ->
                        interoperabilityRecordService.setSuspend(ig.getId(), catalogueId, suspended, auth));
        adapterService.getAllEOSCResourcesOfAProvider(providerId, createFacetFilter(catalogueId), auth)
                .getResults()
                .forEach(ad ->
                        adapterService.setSuspend(ad.getId(), catalogueId, suspended, auth));
        deployableApplicationService.getAllEOSCResourcesOfAProvider(providerId, createFacetFilter(catalogueId), auth)
                .getResults()
                .forEach(da ->
                        deployableApplicationService.setSuspend(da.getId(), catalogueId, suspended, auth));
    }

    public void deleteAllCatalogueRelatedResources(String catalogueId, Authentication auth) {
        FacetFilter ff = createCatalogueFilter(catalogueId);
        serviceService.getAll(ff).getResults().forEach(serviceService::delete);
        datasourceService.getAll(ff).getResults().forEach(datasourceService::delete);
        trainingResourceService.getAll(ff).getResults().forEach(trainingResourceService::delete);
        interoperabilityRecordService.getAll(ff).getResults().forEach(interoperabilityRecordService::delete);
        adapterService.getAll(ff).getResults().forEach(adapterService::delete);
        deployableApplicationService.getAll(ff).getResults().forEach(deployableApplicationService::delete);
        organisationService.getAll(ff).getResults().forEach(organisationService::delete);
    }

    public void suspendAllCatalogueRelatedResources(String catalogueId, boolean suspended, Authentication auth) {
        FacetFilter ff = createCatalogueFilter(catalogueId);
        serviceService.getAll(ff).getResults()
                .forEach(s -> serviceService.setSuspend(s.getId(), catalogueId, suspended, auth));
        datasourceService.getAll(ff).getResults()
                .forEach(ds -> datasourceService.setSuspend(ds.getId(), catalogueId, suspended, auth));
        trainingResourceService.getAll(ff).getResults()
                .forEach(tr -> trainingResourceService.setSuspend(tr.getId(), catalogueId, suspended, auth));
        interoperabilityRecordService.getAll(ff).getResults()
                .forEach(ig -> interoperabilityRecordService.setSuspend(ig.getId(), catalogueId, suspended, auth));
        adapterService.getAll(ff).getResults()
                .forEach(ad -> adapterService.setSuspend(ad.getId(), catalogueId, suspended, auth));
        deployableApplicationService.getAll(ff).getResults()
                .forEach(da -> deployableApplicationService.setSuspend(da.getId(), catalogueId, suspended, auth));
        organisationService.getAll(ff).getResults()
                .forEach(org -> organisationService.setSuspend(org.getId(), catalogueId, suspended, auth));
    }

    public CatalogueResources getAllCatalogueResources(String catalogueId) {
        FacetFilter ff = createCatalogueFilter(catalogueId);
        return new CatalogueResources(
                organisationService.getAll(ff).getResults(),
                serviceService.getAll(ff).getResults(),
                datasourceService.getAll(ff).getResults(),
                trainingResourceService.getAll(ff).getResults(),
                interoperabilityRecordService.getAll(ff).getResults(),
                adapterService.getAll(ff).getResults(),
                deployableApplicationService.getAll(ff).getResults()
        );
    }

    private FacetFilter createFacetFilter(String catalogueId) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        if (catalogueId != null && !catalogueId.isBlank()) {
            ff.addFilter("catalogue_id", catalogueId);
        }
        return ff;
    }

    private FacetFilter createCatalogueFilter(String catalogueId) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("catalogue_id", catalogueId);
        ff.addFilter("published", false);
        ff.addFilter("draft", false);
        return ff;
    }
}
