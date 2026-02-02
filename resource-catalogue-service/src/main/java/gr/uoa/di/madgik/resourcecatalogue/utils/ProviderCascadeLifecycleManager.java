package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.DatasourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ProviderCascadeLifecycleManager {

    private final ServiceService serviceService;
    private final DatasourceService datasourceService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;

    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    public ProviderCascadeLifecycleManager(@Lazy ServiceService serviceService,
                                           @Lazy DatasourceService datasourceService,
                                           @Lazy TrainingResourceService trainingResourceService,
                                           @Lazy InteroperabilityRecordService interoperabilityRecordService) {
        this.serviceService = serviceService;
        this.datasourceService = datasourceService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
    }

    //TODO: populate
    public void deleteAllRelatedResources(ProviderBundle bundle, Authentication auth) {
        String providerId = bundle.getId();
        String catalogueId = bundle.getCatalogueId();
        serviceService.getAllEOSCResourcesOfAProvider(providerId, catalogueId, maxQuantity, auth)
                .getResults()
                .forEach(serviceService::delete);
        datasourceService.getAllEOSCResourcesOfAProvider(providerId, catalogueId, maxQuantity, auth)
                .getResults()
                .forEach(datasourceService::delete);
        trainingResourceService.getAllEOSCResourcesOfAProvider(providerId, catalogueId, maxQuantity, auth)
                .getResults()
                .forEach(trainingResourceService::delete);
        interoperabilityRecordService.getAllEOSCResourcesOfAProvider(providerId, catalogueId, maxQuantity, auth)
                .getResults()
                .forEach(interoperabilityRecordService::delete);
    }

    //TODO: populate
    public void suspendAllRelatedResources(ProviderBundle bundle, Authentication auth) {
        String providerId = bundle.getId();
        String catalogueId = bundle.getCatalogueId();
        boolean suspended = bundle.isSuspended();
        serviceService.getAllEOSCResourcesOfAProvider(providerId, catalogueId, maxQuantity, auth)
                .getResults()
                .forEach(s ->
                        serviceService.setSuspend(s.getId(), catalogueId, suspended, auth));
        datasourceService.getAllEOSCResourcesOfAProvider(providerId, catalogueId, maxQuantity, auth)
                .getResults()
                .forEach(ds ->
                        datasourceService.setSuspend(ds.getId(), catalogueId, suspended, auth));
        trainingResourceService.getAllEOSCResourcesOfAProvider(providerId, catalogueId, maxQuantity, auth)
                .getResults()
                .forEach(tr ->
                        trainingResourceService.setSuspend(tr.getId(), catalogueId, suspended, auth));
        interoperabilityRecordService.getAllEOSCResourcesOfAProvider(providerId, catalogueId, maxQuantity, auth)
                .getResults()
                .forEach(ig ->
                        interoperabilityRecordService.setSuspend(ig.getId(), catalogueId, suspended, auth));
    }
}
