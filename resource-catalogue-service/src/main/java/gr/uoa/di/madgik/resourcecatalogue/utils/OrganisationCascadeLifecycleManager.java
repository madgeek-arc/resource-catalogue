package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class OrganisationCascadeLifecycleManager {

    private final ServiceService serviceService;
    private final DatasourceService datasourceService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final AdapterService adapterService;
    private final DeployableApplicationService deployableApplicationService;

    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    public OrganisationCascadeLifecycleManager(@Lazy ServiceService serviceService,
                                               @Lazy DatasourceService datasourceService,
                                               @Lazy TrainingResourceService trainingResourceService,
                                               @Lazy InteroperabilityRecordService interoperabilityRecordService,
                                               @Lazy AdapterService adapterService,
                                               @Lazy DeployableApplicationService deployableApplicationService) {
        this.serviceService = serviceService;
        this.datasourceService = datasourceService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.adapterService = adapterService;
        this.deployableApplicationService = deployableApplicationService;
    }

    //TODO: catalogue
    public void deleteAllRelatedResources(OrganisationBundle bundle, Authentication auth) {
        String providerId = bundle.getId();
        String catalogueId = bundle.getCatalogueId();
        serviceService.getAllEOSCResourcesOfAProvider(providerId, createFacetFilter(catalogueId), auth)
                .getResults()
                .forEach(serviceService::delete);
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

    //TODO: catalogue
    public void suspendAllRelatedResources(OrganisationBundle bundle, Authentication auth) {
        String providerId = bundle.getId();
        String catalogueId = bundle.getCatalogueId();
        boolean suspended = bundle.isSuspended();
        serviceService.getAllEOSCResourcesOfAProvider(providerId, createFacetFilter(catalogueId), auth)
                .getResults()
                .forEach(s ->
                        serviceService.setSuspend(s.getId(), catalogueId, suspended, auth));
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

    private FacetFilter createFacetFilter(String catalogueId) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        if (catalogueId != null && !catalogueId.isBlank()) {
            ff.addFilter("catalogue_id", catalogueId);
        }
        return ff;
    }
}
