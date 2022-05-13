package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.InfraServiceService;
import eu.einfracentral.registry.service.MonitoringService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static eu.einfracentral.config.CacheConfig.CACHE_MONITORINGS;

@org.springframework.stereotype.Service("monitoringManager")
public class MonitoringManager<T extends Identifiable> extends ResourceManager<Monitoring> implements MonitoringService<Monitoring, Authentication> {

    private static final Logger logger = LogManager.getLogger(MonitoringManager.class);
    private final InfraServiceService<InfraService, InfraService> infraServiceService;
    private RestTemplate restTemplate;
    private HttpHeaders headers;

    public MonitoringManager(InfraServiceService<InfraService, InfraService> infraServiceService) {
        super(Monitoring.class);
        this.infraServiceService = infraServiceService;
    }

    @Override
    public String getResourceType() {
        return "monitoring";
    }

    @Override
    @Cacheable(value = CACHE_MONITORINGS)
    public Monitoring get(String id) {
        return super.get(id);
    }

    @Override
    @CacheEvict(value = CACHE_MONITORINGS, allEntries = true)
    public Monitoring add(Monitoring monitoring, Authentication auth) {

        // check if Service exists
        serviceConsistency(monitoring.getService(), monitoring.getCatalogueId());

        // validate serviceType
        serviceTypeValidation(monitoring);

        monitoring.setId(UUID.randomUUID().toString());
        logger.trace("User '{}' is attempting to add a new Monitoring: {}", auth, monitoring);
        //TODO: metadata
        //TODO: loggingInfo

        super.add(monitoring, auth);
        logger.debug("Adding Monitoring: {}", monitoring);

        //TODO: send emails

        //TODO: jms?

        return monitoring;
    }

    @Override
    @CacheEvict(value = CACHE_MONITORINGS, allEntries = true)
    public Monitoring update(Monitoring monitoring, Authentication auth) {

        logger.trace("User '{}' is attempting to update the Monitoring with id '{}'", auth, monitoring.getId());
        //TODO: metadata
        //TODO: loggingInfo

        super.update(monitoring, null);
        logger.debug("Updating Monitoring: {}", monitoring);

        //TODO: send emails

        //TODO: jms?

        return monitoring;
    }

    @CacheEvict(value = CACHE_MONITORINGS, allEntries = true)
    public void delete(Monitoring monitoring, Authentication auth) {
        logger.trace("User '{}' is attempting to delete the Monitoring with id '{}'", auth, monitoring.getId());

        super.delete(monitoring);
        logger.debug("Deleting Monitoring: {}", monitoring);

        //TODO: send emails

        //TODO: jms?

    }

    public void serviceConsistency(String serviceId, String catalogueId){
        try{
            infraServiceService.get(serviceId, catalogueId);
        } catch(ResourceNotFoundException e){
            throw new ValidationException(String.format("There is no Service with id '%s' in the '%s' Catalogue", serviceId, catalogueId));
        }
    }

    public void serviceTypeValidation(Monitoring monitoring){
        List<String> serviceTypeList = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-api-key", "71553f86c28d296daa4b997bd140015cdeacdb66659aae8b2661c098235ef5ff");
        headers.add("Accept", "application/json");
        String url = "https://api.devel.argo.grnet.gr/api/v2/topology/service-types";
        HttpEntity<String> entity = new HttpEntity<>("body", headers);
        String response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
        JSONObject obj = new JSONObject(response);
        JSONArray arr =  obj.getJSONArray("data");
        for (int i = 0; i < arr.length(); i++)
        {
            serviceTypeList.add(arr.getJSONObject(i).getString("name"));
        }
        for (MonitoringGroup monitoringGroup : monitoring.getMonitoringGroups()){
            String serviceType = monitoringGroup.getServiceType();
            if (!serviceTypeList.contains(serviceType)){
                throw new ValidationException(String.format("The serviceType you provided is wrong. Available serviceTypes are: '%s'", serviceTypeList));
            }
        }
    }
}
