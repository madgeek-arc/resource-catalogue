package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ResourceBundleService;
import eu.einfracentral.registry.service.MonitoringService;
import eu.einfracentral.service.RegistrationMailService;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@org.springframework.stereotype.Service("monitoringManager")
public class MonitoringManager extends ResourceManager<MonitoringBundle> implements MonitoringService<MonitoringBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(MonitoringManager.class);
    private final ResourceBundleService<ServiceBundle> serviceBundleService;
    private final ResourceBundleService<DatasourceBundle> datasourceBundleService;
    private final JmsTemplate jmsTopicTemplate;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final AbstractConsistencyManager abstractConsistencyManager;

    public MonitoringManager(ResourceBundleService<ServiceBundle> serviceBundleService,
                             ResourceBundleService<DatasourceBundle> datasourceBundleService,
                             JmsTemplate jmsTopicTemplate, @Lazy SecurityService securityService,
                             @Lazy RegistrationMailService registrationMailService,
                             @Lazy AbstractConsistencyManager abstractConsistencyManager) {
        super(MonitoringBundle.class);
        this.serviceBundleService = serviceBundleService;
        this.datasourceBundleService = datasourceBundleService;
        this.jmsTopicTemplate = jmsTopicTemplate;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.abstractConsistencyManager = abstractConsistencyManager;
    }

    @Override
    public String getResourceType() {
        return "monitoring";
    }

    @Override
    public MonitoringBundle add(MonitoringBundle monitoring, String resourceType, Authentication auth) {

        // check if Resource exists and if User belongs to Resource's Provider Admins
        if (resourceType.equals("service")){
            abstractConsistencyManager.serviceConsistency(monitoring.getMonitoring().getServiceId(), monitoring.getCatalogueId(), getResourceType());
        } else if (resourceType.equals("datasource")){
            abstractConsistencyManager.datasourceConsistency(monitoring.getMonitoring().getServiceId(), monitoring.getCatalogueId(), getResourceType());
        } else{
            throw new ValidationException("Field resourceType should be either 'service' or 'datasource'");
        }
        validate(monitoring);

        // validate serviceType
        serviceTypeValidation(monitoring.getMonitoring());

        monitoring.setId(UUID.randomUUID().toString());
        logger.trace("User '{}' is attempting to add a new Monitoring: {}", auth, monitoring);

        monitoring.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));
        LoggingInfo loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey());
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        loggingInfoList.add(loggingInfo);
        monitoring.setLoggingInfo(loggingInfoList);
        monitoring.setActive(true);
        // latestOnboardingInfo
        monitoring.setLatestOnboardingInfo(loggingInfo);

        MonitoringBundle ret;
        ret = super.add(monitoring, null);
        logger.debug("Adding Monitoring: {}", monitoring);

        registrationMailService.sendEmailsForMonitoringExtension(monitoring, "post");
        logger.info("Sending JMS with topic 'monitoring.create'");
        jmsTopicTemplate.convertAndSend("monitoring.create", monitoring);

        return ret;
    }

    @Override
    public MonitoringBundle update(MonitoringBundle monitoring, Authentication auth) {
        logger.trace("User '{}' is attempting to update the Monitoring with id '{}'", auth, monitoring.getId());

        Resource existing = whereID(monitoring.getId(), true);
        MonitoringBundle ex = deserialize(existing);
        // check if there are actual changes in the Monitoring
        if (monitoring.getMonitoring().equals(ex.getMonitoring())){
            throw new ValidationException("There are no changes in the Monitoring", HttpStatus.OK);
        }

        validate(monitoring);
        monitoring.setMetadata(Metadata.updateMetadata(monitoring.getMetadata(), User.of(auth).getFullName(), User.of(auth).getEmail()));
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        LoggingInfo loggingInfo;
        loggingInfo = LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
                LoggingInfo.Types.UPDATE.getKey(), LoggingInfo.ActionType.UPDATED.getKey());
        if (monitoring.getLoggingInfo() != null) {
            loggingInfoList = monitoring.getLoggingInfo();
            loggingInfoList.add(loggingInfo);
        } else {
            loggingInfoList.add(loggingInfo);
        }
        monitoring.setLoggingInfo(loggingInfoList);

        // latestUpdateInfo
        monitoring.setLatestUpdateInfo(loggingInfo);

        monitoring.setActive(ex.isActive());
        existing.setPayload(serialize(monitoring));
        existing.setResourceType(resourceType);

        // block user from updating serviceId
        if (!monitoring.getMonitoring().getServiceId().equals(ex.getMonitoring().getServiceId()) && !securityService.hasRole(auth, "ROLE_ADMIN")){
            throw new ValidationException("You cannot change the Service Id with which this Monitoring is related");
        }

        resourceService.updateResource(existing);
        logger.debug("Updating Monitoring: {}", monitoring);

        registrationMailService.sendEmailsForMonitoringExtension(monitoring, "put");
        logger.info("Sending JMS with topic 'monitoring.update'");
        jmsTopicTemplate.convertAndSend("monitoring.update", monitoring);

        return monitoring;
    }

    public void delete(MonitoringBundle monitoring, Authentication auth) {
        logger.trace("User '{}' is attempting to delete the Monitoring with id '{}'", auth, monitoring.getId());

        super.delete(monitoring);
        logger.debug("Deleting Monitoring: {}", monitoring);

        //TODO: send emails
        logger.info("Sending JMS with topic 'monitoring.delete'");
        jmsTopicTemplate.convertAndSend("monitoring.delete", monitoring);

    }

//    public void serviceConsistency(String serviceId, String catalogueId){
//        ServiceBundle serviceBundle;
//        // check if Service exists
//        try{
//            serviceBundle = serviceBundleService.get(serviceId, catalogueId);
//            // check if Service is Public
//            if (serviceBundle.getMetadata().isPublished()){
//                throw new ValidationException("Please provide a Service ID with no catalogue prefix.");
//            }
//        } catch(ResourceNotFoundException e){
//            throw new ValidationException(String.format("There is no Service with id '%s' in the '%s' Catalogue", serviceId, catalogueId));
//        }
//        // check if Service is Active + Approved
//        if (!serviceBundle.isActive() || !serviceBundle.getStatus().equals("approved resource")){
//            throw new ValidationException(String.format("Service with ID [%s] is not Approved and/or Active", serviceId));
//        }
//        // check if Service has already a Monitoring/Helpdesk/ResourceInteroperabilityRecord registered
//        FacetFilter ff = new FacetFilter();
//        ff.setQuantity(maxQuantity);
//        List<MonitoringBundle> allMonitorings = getAll(ff, null).getResults();
//        for (MonitoringBundle monitoring : allMonitorings){
//            if (monitoring.getMonitoring().getServiceId().equals(serviceId) && monitoring.getCatalogueId().equals(catalogueId)){
//                throw new ValidationException(String.format("Service [%s] of the Catalogue [%s] has already a Monitoring " +
//                        "registered, with id: [%s]", serviceId, catalogueId, monitoring.getId()));
//            }
//        }
//    }

//    public void datasourceConsistency(String serviceId, String catalogueId){
//        DatasourceBundle datasourceBundle;
//        // check if Datasource exists
//        try{
//            datasourceBundle = datasourceBundleService.get(serviceId, catalogueId);
//            // check if Datasource is Public
//            if (datasourceBundle.getMetadata().isPublished()){
//                throw new ValidationException("Please provide a Datasource ID with no catalogue prefix.");
//            }
//        } catch(ResourceNotFoundException e){
//            throw new ValidationException(String.format("There is no Datasource with id '%s' in the '%s' Catalogue", serviceId, catalogueId));
//        }
//        // check if Datasource is Active + Approved
//        if (!datasourceBundle.isActive() || !datasourceBundle.getStatus().equals("approved resource")){
//            throw new ValidationException(String.format("Datasource with ID [%s] is not Approved and/or Active", serviceId));
//        }
//        // check if Datasource has already a Monitoring registered
//        FacetFilter ff = new FacetFilter();
//        ff.setQuantity(maxQuantity);
//        List<MonitoringBundle> allMonitorings = getAll(ff, null).getResults();
//        for (MonitoringBundle monitoring : allMonitorings){
//            if (monitoring.getMonitoring().getServiceId().equals(serviceId) && monitoring.getCatalogueId().equals(catalogueId)){
//                throw new ValidationException(String.format("Datasource [%s] of the Catalogue [%s] has already a Monitoring " +
//                        "registered, with id: [%s]", serviceId, catalogueId, monitoring.getId()));
//            }
//        }
//    }

    public List<String> getAvailableServiceTypes() {
        List<String> serviceTypeList = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-api-key", "71553f86c28d296daa4b997bd140015cdeacdb66659aae8b2661c098235ef5ff");
        headers.add("Accept", "application/json");
        String url = "https://api.devel.argo.grnet.gr/api/v2/topology/service-types";
        HttpEntity<String> entity = new HttpEntity<>("body", headers);
        String response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
        JSONObject obj = new JSONObject(response);
        JSONArray arr = obj.getJSONArray("data");
        for (int i = 0; i < arr.length(); i++) {
            serviceTypeList.add(arr.getJSONObject(i).getString("name"));
        }
        return serviceTypeList;
    }

    public void serviceTypeValidation(Monitoring monitoring){
        List<String> serviceTypeList = getAvailableServiceTypes();
        for (MonitoringGroup monitoringGroup : monitoring.getMonitoringGroups()){
            String serviceType = monitoringGroup.getServiceType();
            if (!serviceTypeList.contains(serviceType)){
                throw new ValidationException(String.format("The serviceType you provided is wrong. Available serviceTypes are: '%s'", serviceTypeList));
            }
        }
    }
}
