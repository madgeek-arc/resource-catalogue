package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.Identifiers;
import eu.einfracentral.domain.interoperabilityRecord.configurationTemplates.ConfigurationTemplateInstanceBundle;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.exception.ResourceNotFoundException;
import eu.einfracentral.service.SecurityService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.ResourceCRUDService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import eu.einfracentral.utils.JmsService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service("publicConfigurationTemplateImplementationManager")
public class PublicConfigurationTemplateImplementationManager extends ResourceManager<ConfigurationTemplateInstanceBundle>
        implements ResourceCRUDService<ConfigurationTemplateInstanceBundle, Authentication> {

    private static final Logger logger = LogManager.getLogger(PublicConfigurationTemplateImplementationManager.class);
    private final JmsService jmsService;
    private final SecurityService securityService;
    @Value("${project.catalogue.name}")
    private String catalogueName;


    @Autowired
    public PublicConfigurationTemplateImplementationManager(JmsService jmsService, SecurityService securityService) {
        super(ConfigurationTemplateInstanceBundle.class);
        this.jmsService = jmsService;
        this.securityService = securityService;
    }

    @Override
    public String getResourceType() {
        return "configuration_template_instance";
    }

    @Override
    public Browsing<ConfigurationTemplateInstanceBundle> getAll(FacetFilter facetFilter, Authentication authentication) {
        return super.getAll(facetFilter, authentication);
    }

    // TODO: Refactor id, resourceId, payload's interoperabilityRecordId creations when more Catalogues are supported
    // TODO: (need to have catalogueId in CTI model)
    @Override
    public ConfigurationTemplateInstanceBundle add(ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle, Authentication authentication) {
        String lowerLevelResourceId = configurationTemplateInstanceBundle.getId();
        Identifiers.createOriginalId(configurationTemplateInstanceBundle);
        configurationTemplateInstanceBundle.setId(String.format("%s.%s", catalogueName, configurationTemplateInstanceBundle.getId()));
        configurationTemplateInstanceBundle.getConfigurationTemplateInstance().setResourceId(String.format("%s.%s", catalogueName,
                configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getResourceId()));
        JSONParser parser = new JSONParser();
        try {
            JSONObject payload = (JSONObject) parser.parse(configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getPayload().replaceAll("'", "\""));
            payload.put("interoperabilityRecordId", catalogueName + "." + payload.get("interoperabilityRecordId"));
            configurationTemplateInstanceBundle.getConfigurationTemplateInstance().setPayload(payload.toString());
        } catch (ParseException e) {
            //continue
        }
        configurationTemplateInstanceBundle.getMetadata().setPublished(true);
        ConfigurationTemplateInstanceBundle ret;
        logger.info(String.format("ConfigurationTemplateInstanceBundle [%s] is being published with id [%s]", lowerLevelResourceId, configurationTemplateInstanceBundle.getId()));
        ret = super.add(configurationTemplateInstanceBundle, null);
        jmsService.convertAndSendTopic("configuration_template_instance.create", configurationTemplateInstanceBundle);
        return ret;
    }

    @Override
    public ConfigurationTemplateInstanceBundle update(ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle, Authentication authentication) {
        ConfigurationTemplateInstanceBundle published = super.get(String.format("%s.%s", catalogueName, configurationTemplateInstanceBundle.getId()));
        ConfigurationTemplateInstanceBundle ret = super.get(String.format("%s.%s", catalogueName, configurationTemplateInstanceBundle.getId()));
        try {
            BeanUtils.copyProperties(ret, configurationTemplateInstanceBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        ret.getConfigurationTemplateInstance().setResourceId(String.format("%s.%s", catalogueName,
                configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getResourceId()));
        ret.setIdentifiers(published.getIdentifiers());
        ret.getConfigurationTemplateInstance().setPayload(published.getConfigurationTemplateInstance().getPayload()); //TODO: refactor when users will be able to update CTIs
        ret.setId(published.getId());
        ret.getMetadata().setPublished(true);
        logger.info(String.format("Updating public ResourceInteroperabilityRecordBundle with id [%s]", ret.getId()));
        ret = super.update(ret, null);
        jmsService.convertAndSendTopic("resource_interoperability_record.update", ret);
        return ret;
    }

    @Override
    public void delete(ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle) {
        try{
            ConfigurationTemplateInstanceBundle publicConfigurationTemplateInstanceBundle = get(String.format("%s.%s",
                    catalogueName, configurationTemplateInstanceBundle.getId()));
            logger.info(String.format("Deleting public ConfigurationTemplateInstanceBundle with id [%s]", publicConfigurationTemplateInstanceBundle.getId()));
            super.delete(publicConfigurationTemplateInstanceBundle);
            jmsService.convertAndSendTopic("configuration_template_instance.delete", publicConfigurationTemplateInstanceBundle);
        } catch (ResourceException | ResourceNotFoundException ignore){
        }
    }

}
