/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.manager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.exception.ResourceAlreadyExistsException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.dto.MonitoringStatus;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.AuthenticationInfo;
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
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;


@org.springframework.stereotype.Service("monitoringManager")
public class MonitoringManager extends ResourceCatalogueManager<MonitoringBundle> implements MonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringManager.class);
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final PublicMonitoringService publicMonitoringManager;
    private final SecurityService securityService;
    private final RegistrationMailService registrationMailService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final WebClient webClient;

    @Value("${catalogue.id}")
    private String catalogueId;

    @Value("${argo.grnet.monitoring.token:}")
    private String monitoringToken;
    @Value("${argo.grnet.monitoring.service.types:}")
    private String monitoringServiceTypes;

    private final IdCreator idCreator;


    public MonitoringManager(ServiceBundleService<ServiceBundle> serviceBundleService,
                             TrainingResourceService trainingResourceService,
                             PublicMonitoringService publicMonitoringManager,
                             @Lazy SecurityService securityService,
                             @Lazy RegistrationMailService registrationMailService,
                             ProviderResourcesCommonMethods commonMethods,
                             IdCreator idCreator,
                             WebClient.Builder webClientBuilder) {
        super(MonitoringBundle.class);
        this.serviceBundleService = serviceBundleService;
        this.trainingResourceService = trainingResourceService;
        this.publicMonitoringManager = publicMonitoringManager;
        this.securityService = securityService;
        this.registrationMailService = registrationMailService;
        this.commonMethods = commonMethods;
        this.idCreator = idCreator;
        this.webClient = webClientBuilder.build();
    }

    @Override
    public String getResourceTypeName() {
        return "monitoring";
    }

    @Override
    public MonitoringBundle validate(MonitoringBundle monitoringBundle, String resourceType) {
        String resourceId = monitoringBundle.getMonitoring().getServiceId();
        String catalogueId = monitoringBundle.getMonitoring().getCatalogueId();

        MonitoringBundle existingMonitoring = get(resourceId, catalogueId);
        if (existingMonitoring != null) {
            throw new ResourceAlreadyExistsException(
                    String.format("Resource [%s] of the Catalogue [%s] has already a Monitoring " +
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
        if (monitoring.getMonitoring().getCatalogueId() == null || monitoring.getMonitoring().getCatalogueId().isEmpty()) {
            // set catalogueId = eosc
            monitoring.getMonitoring().setCatalogueId(catalogueId);
        }

        validate(monitoring, resourceType);

        monitoring.setId(idCreator.generate(getResourceTypeName()));
        commonMethods.createIdentifiers(monitoring, getResourceTypeName(), false);
        logger.trace("Attempting to add a new Monitoring: {}", monitoring);

        monitoring.setMetadata(Metadata.createMetadata(AuthenticationInfo.getFullName(auth),
                AuthenticationInfo.getEmail(auth).toLowerCase()));
        List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(monitoring, auth);
        monitoring.setLoggingInfo(loggingInfoList);
        monitoring.setActive(true);
        // latestOnboardingInfo
        monitoring.setLatestOnboardingInfo(loggingInfoList.getFirst());
        // default monitoredBy value -> EOSC
        monitoring.getMonitoring().setMonitoredBy("monitored_by-eosc");

        MonitoringBundle ret;
        ret = super.add(monitoring, null);
        logger.info("Added Monitoring with id '{}'", monitoring.getId());

        registrationMailService.sendEmailsForMonitoringExtensionToPortalAdmins(monitoring, "post");

        return ret;
    }

    @Override
    public MonitoringBundle update(MonitoringBundle monitoringBundle, Authentication auth) {
        logger.trace("Attempting to update the Monitoring with id '{}'", monitoringBundle.getId());

        MonitoringBundle ret = ObjectUtils.clone(monitoringBundle);
        Resource existingResource = getResource(monitoringBundle.getId(),
                monitoringBundle.getMonitoring().getCatalogueId(), false);
        MonitoringBundle existingMonitoring = deserialize(existingResource);
        // check if there are actual changes in the Monitoring
        if (ret.getMonitoring().equals(existingMonitoring.getMonitoring())) {
            return ret;
        }

        validate(ret);
        ret.setMetadata(Metadata.updateMetadata(ret.getMetadata(), AuthenticationInfo.getFullName(auth),
                AuthenticationInfo.getEmail(auth).toLowerCase()));
        ret.setIdentifiers(existingMonitoring.getIdentifiers());
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
        existingResource.setResourceType(getResourceType());

        // block user from updating serviceId
        if (!ret.getMonitoring().getServiceId().equals(existingMonitoring.getMonitoring().getServiceId()) &&
                !securityService.hasRole(auth, "ROLE_ADMIN")) {
            throw new ValidationException("You cannot change the Service Id with which this Monitoring is related");
        }

        resourceService.updateResource(existingResource);
        logger.info("Updated Monitoring with id '{}'", ret.getId());

        registrationMailService.sendEmailsForMonitoringExtensionToPortalAdmins(ret, "put");

        return ret;
    }

    public void updateBundle(MonitoringBundle monitoringBundle, Authentication auth) {
        logger.trace("Attempting to update the Monitoring: {}", monitoringBundle);

        Resource existing = getResource(monitoringBundle.getId(),
                monitoringBundle.getMonitoring().getCatalogueId(), false);
        if (existing == null) {
            throw new ResourceNotFoundException(monitoringBundle.getId(), "Monitoring");
        }

        existing.setPayload(serialize(monitoringBundle));
        existing.setResourceType(getResourceType());

        resourceService.updateResource(existing);
    }

    @Override
    public void delete(MonitoringBundle monitoring) {
        super.delete(monitoring);
        logger.info("Deleted the Monitoring with id '{}' of the Catalogue '{}'",
                monitoring.getMonitoring().getId(), monitoring.getMonitoring().getCatalogueId());
    }

    public List<Vocabulary> getAvailableServiceTypes() {
        String response = callMonitoringApi(monitoringServiceTypes, monitoringToken);
        if (response == null || response.isEmpty()) return Collections.emptyList();

        JSONObject obj = new JSONObject(response);
        JSONArray array = obj.getJSONArray("data");
        return createServiceTypeVocabularyList(array);
    }

    private List<Vocabulary> createServiceTypeVocabularyList(JSONArray array) {
        List<Vocabulary> serviceTypeList = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            String date = array.getJSONObject(i).get("date").toString();
            String name = array.getJSONObject(i).get("name").toString();
            String title = array.getJSONObject(i).get("title").toString();
            String description = array.getJSONObject(i).get("description").toString();
            JSONArray tagsArray = array.getJSONObject(i).getJSONArray("tags");
            List<String> tags = new ArrayList<>();
            for (int j = 0; j < tagsArray.length(); j++) {
                tags.add(tagsArray.getString(j));
            }
            String tagsString = String.join(",", tags);
            Map<String, String> extras = new HashMap<>();
            extras.put("date", date);
            extras.put("tags", tagsString);
            Vocabulary vocabulary = new Vocabulary(name, description, description, null,
                    "external-monitoring_service_type", extras);
            serviceTypeList.add(vocabulary);
        }
        return serviceTypeList;
    }

    @Override
    public MonitoringBundle get(String serviceId, String catalogueId) {
        Resource res = where(false,
                new SearchService.KeyValue("service_id", serviceId),
                new SearchService.KeyValue("catalogue_id", catalogueId),
                new SearchService.KeyValue("published", "false"));
        return res != null ? deserialize(res) : null;
    }

    public void serviceTypeValidation(Monitoring monitoring) {
        List<Vocabulary> serviceTypeList = getAvailableServiceTypes();
        List<String> serviceTypeIds = new ArrayList<>();
        for (Vocabulary type : serviceTypeList) {
            serviceTypeIds.add(type.getId());
        }
        for (MonitoringGroup monitoringGroup : monitoring.getMonitoringGroups()) {
            String serviceType = monitoringGroup.getServiceType();
            if (!serviceTypeIds.contains(serviceType)) {
                throw new ValidationException(String.format("The serviceType you provided is wrong. " +
                        "Available serviceTypes are: '%s'", serviceTypeList));
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
        logger.info("User '{}-{}' attempts to create a Public Monitoring from Monitoring '{}' of the '{}' Catalogue",
                Objects.requireNonNull(AuthenticationInfo.getFullName(auth)),
                Objects.requireNonNull(AuthenticationInfo.getEmail(auth).toLowerCase()),
                monitoringBundle.getId(), monitoringBundle.getMonitoring().getCatalogueId());
        publicMonitoringManager.add(monitoringBundle, auth);
        return monitoringBundle;
    }

    public List<MonitoringStatus> getAvailabilityOrStatus(String url, String field) {
        String response = callMonitoringApi(url, monitoringToken);
        List<MonitoringStatus> serviceMonitoringStatuses = null;
        if (response != null) {
            JSONObject obj = new JSONObject(response);
            Gson gson = new Gson();
            JsonElement jsonObj = gson.fromJson(String.valueOf(obj), JsonElement.class);
            JsonArray results = jsonObj.getAsJsonObject().get("endpoints").getAsJsonArray().get(0).
                    getAsJsonObject().get(field).getAsJsonArray();
            serviceMonitoringStatuses = createMonitoringAvailabilityObject(results);
        }
        return serviceMonitoringStatuses;
    }

    private String callMonitoringApi(String url, String token) {
        return webClient.get()
                .uri(url)
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .header("x-api-key", token)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), clientResponse -> Mono.empty())
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.empty())
                .block();
    }
}
