package gr.uoa.di.madgik.resourcecatalogue.manager;

import com.google.gson.JsonArray;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.dto.MonitoringStatus;
import gr.uoa.di.madgik.resourcecatalogue.dto.ServiceType;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.CreateArgoGrnetHttpRequest;
import gr.uoa.di.madgik.resourcecatalogue.utils.ObjectUtils;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import gr.uoa.di.madgik.resourcecatalogue.utils.ResourceValidationUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;


@org.springframework.stereotype.Service("monitoringManager")
public class MonitoringManager extends ResourceManager<MonitoringBundle> implements MonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringManager.class);
    private final ServiceBundleService serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final PublicMonitoringManager publicMonitoringManager;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final ProviderResourcesCommonMethods commonMethods;

    @Value("${argo.grnet.monitoring.token}")
    private String monitoringToken;
    @Value("${argo.grnet.monitoring.service.types}")
    private String monitoringServiceTypes;

    private final IdCreator idCreator;


    public MonitoringManager(ServiceBundleService serviceBundleService,
                             TrainingResourceService trainingResourceService,
                             PublicMonitoringManager publicMonitoringManager,
                             @Lazy SecurityService securityService,
                             @Lazy RegistrationMailService registrationMailService,
                             ProviderResourcesCommonMethods commonMethods,
                             IdCreator idCreator) {
        super(MonitoringBundle.class);
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.publicMonitoringManager = publicMonitoringManager;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.commonMethods = commonMethods;
        this.idCreator = idCreator;
    }

    @Override
    public String getResourceType() {
        return "monitoring";
    }

    @Override
    public MonitoringBundle validate(MonitoringBundle monitoringBundle, String resourceType) {
        String resourceId = monitoringBundle.getMonitoring().getServiceId();
        String catalogueId = monitoringBundle.getCatalogueId();

        MonitoringBundle existingMonitoring = get(resourceId, catalogueId);
        if (existingMonitoring != null) {
            throw new ValidationException(String.format("Resource [%s] of the Catalogue [%s] has already a Monitoring " +
                    "registered, with id: [%s]", resourceId, catalogueId, existingMonitoring.getId()));
        }

        // check if Resource exists and if User belongs to Resource's Provider Admins
        if (resourceType.equals("service")) {
            ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(resourceId, catalogueId, serviceBundleService, resourceType);
        } else if (resourceType.equals("training_resource")) {
            ResourceValidationUtils.checkIfResourceBundleIsActiveAndApprovedAndNotPublic(resourceId, catalogueId, trainingResourceService, resourceType);
        } else {
            throw new ValidationException("Field resourceType should be either 'service' or 'training_resource'");
        }

        super.validate(monitoringBundle);

        // validate serviceType
        serviceTypeValidation(monitoringBundle.getMonitoring());

        return monitoringBundle;
    }

    @Override
    public MonitoringBundle add(MonitoringBundle monitoring, String resourceType, Authentication auth) {
        validate(monitoring, resourceType);

        monitoring.setId(idCreator.generate(getResourceType()));
        logger.trace("Attempting to add a new Monitoring: {}", monitoring);

        monitoring.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(monitoring, auth);
        monitoring.setLoggingInfo(loggingInfoList);
        monitoring.setActive(true);
        // latestOnboardingInfo
        monitoring.setLatestOnboardingInfo(loggingInfoList.get(0));
        // default monitoredBy value -> EOSC
        monitoring.getMonitoring().setMonitoredBy("monitored_by-eosc");

        MonitoringBundle ret;
        ret = super.add(monitoring, null);
        logger.debug("Adding Monitoring: {}", monitoring);

        registrationMailService.sendEmailsForMonitoringExtension(monitoring, resourceType, "post");

        return ret;
    }

    @Override
    public MonitoringBundle update(MonitoringBundle monitoringBundle, Authentication auth) {
        logger.trace("Attempting to update the Monitoring with id '{}'", monitoringBundle.getId());

        MonitoringBundle ret = ObjectUtils.clone(monitoringBundle);
        Resource existingResource = whereID(ret.getId(), true);
        MonitoringBundle existingMonitoring = deserialize(existingResource);
        // check if there are actual changes in the Monitoring
        if (ret.getMonitoring().equals(existingMonitoring.getMonitoring())) {
            return ret;
        }

        validate(ret);
        ret.setMetadata(Metadata.updateMetadata(ret.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(ret, auth);
        LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                LoggingInfo.ActionType.UPDATED.getKey());
        loggingInfoList.add(loggingInfo);
        ret.setLoggingInfo(loggingInfoList);

        // latestLoggingInfo
        ret.setLatestUpdateInfo(loggingInfo);
        ret.setLatestOnboardingInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.ONBOARD.getKey()));
        ret.setLatestAuditInfo(commonMethods.setLatestLoggingInfo(loggingInfoList, LoggingInfo.Types.AUDIT.getKey()));

        // default monitoredBy value -> EOSC
        ret.getMonitoring().setMonitoredBy("monitored_by-eosc");

        ret.setActive(existingMonitoring.isActive());
        existingResource.setPayload(serialize(ret));
        existingResource.setResourceType(resourceType);

        // block user from updating serviceId
        if (!ret.getMonitoring().getServiceId().equals(existingMonitoring.getMonitoring().getServiceId()) && !securityService.hasRole(auth, "ROLE_ADMIN")) {
            throw new ValidationException("You cannot change the Service Id with which this Monitoring is related");
        }

        resourceService.updateResource(existingResource);
        logger.debug("Updating Monitoring: {}", ret);

        registrationMailService.sendEmailsForMonitoringExtension(ret, "Resource", "put");

        return ret;
    }

    public void updateBundle(MonitoringBundle monitoringBundle, Authentication auth) {
        logger.trace("Attempting to update the Monitoring: {}", monitoringBundle);

        Resource existing = getResource(monitoringBundle.getId());
        if (existing == null) {
            throw new ResourceNotFoundException(
                    String.format("Could not update Monitoring with id '%s' because it does not exist",
                            monitoringBundle.getId()));
        }

        existing.setPayload(serialize(monitoringBundle));
        existing.setResourceType(resourceType);

        resourceService.updateResource(existing);
    }

    @Override
    public void delete(MonitoringBundle monitoring) {
        super.delete(monitoring);
        logger.debug("Deleting Monitoring: {}", monitoring);
    }

    public List<ServiceType> getAvailableServiceTypes() {
        List<ServiceType> serviceTypeList = new ArrayList<>();
        String response = CreateArgoGrnetHttpRequest.createHttpRequest(monitoringServiceTypes, monitoringToken);
        JSONObject obj = new JSONObject(response);
        JSONArray arr = obj.getJSONArray("data");
        for (int i = 0; i < arr.length(); i++) {
            String date = arr.getJSONObject(i).get("date").toString();
            String name = arr.getJSONObject(i).get("name").toString();
            String title = arr.getJSONObject(i).get("title").toString();
            String description = arr.getJSONObject(i).get("description").toString();
            ServiceType serviceType = new ServiceType(date, name, title, description);
            serviceTypeList.add(serviceType);
        }
        return serviceTypeList;
    }

    @Override
    public MonitoringBundle get(String serviceId, String catalogueId) {
        Resource res = where(false, new SearchService.KeyValue("service_id", serviceId), new SearchService.KeyValue("catalogue_id", catalogueId));
        return res != null ? deserialize(res) : null;
    }

    public void serviceTypeValidation(Monitoring monitoring) {
        List<ServiceType> serviceTypeList = getAvailableServiceTypes();
        List<String> serviceTypeNames = new ArrayList<>();
        for (ServiceType type : serviceTypeList) {
            serviceTypeNames.add(type.getName());
        }
        for (MonitoringGroup monitoringGroup : monitoring.getMonitoringGroups()) {
            String serviceType = monitoringGroup.getServiceType();
            if (!serviceTypeNames.contains(serviceType)) {
                throw new ValidationException(String.format("The serviceType you provided is wrong. Available serviceTypes are: '%s'", serviceTypeList));
            }
        }
    }


    // Argo GRNET Monitoring Status methods
    public List<MonitoringStatus> createMonitoringAvailabilityObject(JsonArray results) {
        List<MonitoringStatus> monitoringStatuses = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            String date = results.get(i).getAsJsonObject().get("date").getAsString();
            String availability = results.get(i).getAsJsonObject().get("availability").getAsString();
            String reliability = results.get(i).getAsJsonObject().get("reliability").getAsString();
            String unknown = results.get(i).getAsJsonObject().get("unknown").getAsString();
            String uptime = results.get(i).getAsJsonObject().get("uptime").getAsString();
            String downtime = results.get(i).getAsJsonObject().get("downtime").getAsString();
            MonitoringStatus monitoringStatus =
                    new MonitoringStatus(date, availability, reliability, unknown, uptime, downtime);
            monitoringStatuses.add(monitoringStatus);
        }
        return monitoringStatuses;
    }

    public List<MonitoringStatus> createMonitoringStatusObject(JsonArray results) {
        List<MonitoringStatus> monitoringStatuses = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            String timestamp = results.get(i).getAsJsonObject().get("timestamp").getAsString();
            String value = results.get(i).getAsJsonObject().get("value").getAsString();
            MonitoringStatus monitoringStatus =
                    new MonitoringStatus(timestamp, value);
            monitoringStatuses.add(monitoringStatus);
        }
        return monitoringStatuses;
    }

    public MonitoringBundle createPublicResource(MonitoringBundle monitoringBundle, Authentication auth) {
        publicMonitoringManager.add(monitoringBundle, auth);
        return monitoringBundle;
    }
}
