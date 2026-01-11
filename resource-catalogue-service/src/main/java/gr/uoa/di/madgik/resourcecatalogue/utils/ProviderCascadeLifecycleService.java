package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.resourcecatalogue.domain.NewProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.service.TrainingResourceService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ProviderCascadeLifecycleService {

    private final ServiceBundleService serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final InteroperabilityRecordService interoperabilityRecordService;

    public ProviderCascadeLifecycleService(ServiceBundleService serviceBundleService,
                                           TrainingResourceService trainingResourceService,
                                           InteroperabilityRecordService interoperabilityRecordService) {
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.interoperabilityRecordService = interoperabilityRecordService;
    }

    //TODO: populate with other resources if needed
    public void deleteAllRelatedResources(NewProviderBundle bundle, Authentication auth) {
        String id = bundle.getId();
        String catalogueId = bundle.getCatalogueId();
        serviceBundleService.getResourceBundles(catalogueId, id, auth)
                .getResults().stream()
                .filter(s -> !s.getMetadata().isPublished())
                .forEach(s -> serviceBundleService.delete(s));
        trainingResourceService.getResourceBundles(catalogueId, id, auth)
                .getResults().stream()
                .filter(tr -> !tr.getMetadata().isPublished())
                .forEach(tr -> trainingResourceService.delete(tr));
        interoperabilityRecordService.getInteroperabilityRecordBundles(catalogueId, id, auth)
                .getResults().stream()
                .filter(ig -> !ig.getMetadata().isPublished())
                .forEach(ig -> interoperabilityRecordService.delete(ig));
    }

    //TODO: populate with other resources if needed
    public void suspendAllRelatedResources(NewProviderBundle bundle, Authentication auth) {
        String id = bundle.getId();
        String catalogueId = bundle.getCatalogueId();
        boolean suspended = bundle.isSuspended();
        serviceBundleService.getResourceBundles(catalogueId, id, auth)
                .getResults().stream()
                .filter(s -> !s.getMetadata().isPublished())
                .forEach(s ->
                        serviceBundleService.suspend(s.getId(), catalogueId, suspended, auth)
                );
        trainingResourceService.getResourceBundles(catalogueId, id, auth)
                .getResults().stream()
                .filter(tr -> !tr.getMetadata().isPublished())
                .forEach(tr ->
                        trainingResourceService.suspend(tr.getId(), catalogueId, suspended, auth)
                );
        interoperabilityRecordService
                .getInteroperabilityRecordBundles(catalogueId, id, auth)
                .getResults().stream()
                .filter(ir -> !ir.getMetadata().isPublished())
                .forEach(ir ->
                        interoperabilityRecordService.suspend(ir.getId(), catalogueId, suspended, auth)
                );
    }
}
