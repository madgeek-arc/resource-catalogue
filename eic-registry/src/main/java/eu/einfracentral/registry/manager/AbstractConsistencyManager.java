package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.*;
import eu.openminted.registry.core.domain.FacetFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;

import java.util.List;

@org.springframework.stereotype.Service("abstractConsistencyService")
public class AbstractConsistencyManager {

    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;
    private final ResourceBundleService<ServiceBundle> serviceBundleService;
    private final ResourceBundleService<DatasourceBundle> datasourceBundleService;
    private final HelpdeskService<HelpdeskBundle, Authentication> helpdeskService;
    private final MonitoringService<MonitoringBundle, Authentication> monitoringService;
    private final ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle, Authentication> resourceInteroperabilityRecordService;


    public AbstractConsistencyManager(ResourceBundleService<ServiceBundle> serviceBundleService,
                                      ResourceBundleService<DatasourceBundle> datasourceBundleService,
                                      HelpdeskService<HelpdeskBundle, Authentication> helpdeskService,
                                      MonitoringService<MonitoringBundle, Authentication> monitoringService,
                                      ResourceInteroperabilityRecordService<ResourceInteroperabilityRecordBundle, Authentication> resourceInteroperabilityRecordService) {
        this.serviceBundleService = serviceBundleService;
        this.datasourceBundleService = datasourceBundleService;
        this.helpdeskService = helpdeskService;
        this.monitoringService = monitoringService;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
    }

    public void serviceConsistency(String resourceId, String catalogueId, String resourceType){
        ServiceBundle serviceBundle;
        // check if Service exists
        try{
            serviceBundle = serviceBundleService.get(resourceId, catalogueId);
            // check if Service is Public
            if (serviceBundle.getMetadata().isPublished()){
                throw new ValidationException("Please provide a Service ID with no catalogue prefix.");
            }
        } catch(ResourceNotFoundException e){
            throw new ValidationException(String.format("There is no Service with id '%s' in the '%s' Catalogue", resourceId, catalogueId));
        }
        // check if Service is Active + Approved
        if (!serviceBundle.isActive() || !serviceBundle.getStatus().equals("approved resource")){
            throw new ValidationException(String.format("Service with ID [%s] is not Approved and/or Active", resourceId));
        }
        // check if Service has already a 'resourceType' registered
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        checkResourceRegistries(ff, resourceId, catalogueId, resourceType);
    }

    public void datasourceConsistency(String resourceId, String catalogueId, String resourceType){
        DatasourceBundle datasourceBundle;
        // check if Datasource exists
        try{
            datasourceBundle = datasourceBundleService.get(resourceId, catalogueId);
            // check if Datasource is Public
            if (datasourceBundle.getMetadata().isPublished()){
                throw new ValidationException("Please provide a Datasource ID with no catalogue prefix.");
            }
        } catch(ResourceNotFoundException e){
            throw new ValidationException(String.format("There is no Datasource with id '%s' in the '%s' Catalogue", resourceId, catalogueId));
        }
        // check if Datasource is Active + Approved
        if (!datasourceBundle.isActive() || !datasourceBundle.getStatus().equals("approved resource")){
            throw new ValidationException(String.format("Datasource with ID [%s] is not Approved and/or Active", resourceId));
        }
        // check if Datasource has already a 'resourceType' registered
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        checkResourceRegistries(ff, resourceId, catalogueId, resourceType);
    }

    private void checkResourceRegistries(FacetFilter ff, String resourceId, String catalogueId, String resourceType){
        switch (resourceType){
            case "helpdesk":
                List<HelpdeskBundle> allHelpdesks = helpdeskService.getAll(ff, null).getResults();
                for (HelpdeskBundle helpdesk : allHelpdesks){
                    if (helpdesk.getHelpdesk().getServiceId().equals(resourceId) && helpdesk.getCatalogueId().equals(catalogueId)){
                            throw new ValidationException(String.format("Resource [%s] of the Catalogue [%s] has already a Helpdesk " +
                                "registered, with id: [%s]", resourceId, catalogueId, helpdesk.getId()));
                    }
                }
                break;
            case "monitoring":
                List<MonitoringBundle> allMonitorings = monitoringService.getAll(ff, null).getResults();
                for (MonitoringBundle monitoring : allMonitorings){
                    if (monitoring.getMonitoring().getServiceId().equals(resourceId) && monitoring.getCatalogueId().equals(catalogueId)){
                        throw new ValidationException(String.format("Resource [%s] of the Catalogue [%s] has already a Monitoring " +
                                "registered, with id: [%s]", resourceId, catalogueId, monitoring.getId()));
                    }
                }
            case "resource_interoperability_record":
                List<ResourceInteroperabilityRecordBundle> allResourceInteroperabilityRecords = resourceInteroperabilityRecordService.getAll(ff, null).getResults();
                for (ResourceInteroperabilityRecordBundle resourceInteroperabilityRecord : allResourceInteroperabilityRecords){
                    if (resourceInteroperabilityRecord.getResourceInteroperabilityRecord().getResourceId().equals(resourceId) &&
                            resourceInteroperabilityRecord.getResourceInteroperabilityRecord().getCatalogueId().equals(catalogueId)){
                        throw new ValidationException(String.format("Resource [%s] of the Catalogue [%s] has already a Resource " +
                                        "Interoperability Record registered, with id: [%s]", resourceId, catalogueId,
                                resourceInteroperabilityRecord.getId()));
                    }
                }
        }
    }
}
