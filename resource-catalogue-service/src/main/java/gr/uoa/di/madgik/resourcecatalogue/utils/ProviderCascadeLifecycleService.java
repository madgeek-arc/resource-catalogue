package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class ProviderCascadeLifecycleService {

    private final ServiceService serviceService;

    public ProviderCascadeLifecycleService(@Lazy ServiceService serviceService) {
        this.serviceService = serviceService;
    }

    //TODO: populate with other resources if needed
    //FIXME
//    public void deleteAllRelatedResources(NewProviderBundle bundle, Authentication auth) {
//        String id = bundle.getId();
//        String catalogueId = bundle.getCatalogueId();
//        serviceService.getResourceBundles(catalogueId, id, auth)
//                .getResults().stream()
//                .filter(s -> !s.getMetadata().isPublished())
//                .forEach(s -> serviceService.delete(s));
//        trainingResourceService.getResourceBundles(catalogueId, id, auth)
//                .getResults().stream()
//                .filter(tr -> !tr.getMetadata().isPublished())
//                .forEach(tr -> trainingResourceService.delete(tr));
//        interoperabilityRecordService.getInteroperabilityRecordBundles(catalogueId, id, auth)
//                .getResults().stream()
//                .filter(ig -> !ig.getMetadata().isPublished())
//                .forEach(ig -> interoperabilityRecordService.delete(ig));
//    }

    //TODO: populate with other resources if needed
    //FIXME
//    public void suspendAllRelatedResources(NewProviderBundle bundle, Authentication auth) {
//        String id = bundle.getId();
//        String catalogueId = bundle.getCatalogueId();
//        boolean suspended = bundle.isSuspended();
//        serviceService.getResourceBundles(catalogueId, id, auth)
//                .getResults().stream()
//                .filter(s -> !s.getMetadata().isPublished())
//                .forEach(s ->
//                        serviceService.suspend(s.getId(), catalogueId, suspended, auth)
//                );
//        trainingResourceService.getResourceBundles(catalogueId, id, auth)
//                .getResults().stream()
//                .filter(tr -> !tr.getMetadata().isPublished())
//                .forEach(tr ->
//                        trainingResourceService.suspend(tr.getId(), catalogueId, suspended, auth)
//                );
//        interoperabilityRecordService
//                .getInteroperabilityRecordBundles(catalogueId, id, auth)
//                .getResults().stream()
//                .filter(ir -> !ir.getMetadata().isPublished())
//                .forEach(ir ->
//                        interoperabilityRecordService.suspend(ir.getId(), catalogueId, suspended, auth)
//                );
//    }
}
