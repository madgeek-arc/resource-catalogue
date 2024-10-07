package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.ResourceCRUDService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiers;
import gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.utils.JmsService;
import gr.uoa.di.madgik.resourcecatalogue.utils.PublicResourceUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service("publicConfigurationTemplateInstanceManager")
public class PublicConfigurationTemplateInstanceManager extends AbstractPublicResourceManager<ConfigurationTemplateInstanceBundle>
        implements ResourceCRUDService<ConfigurationTemplateInstanceBundle, Authentication> {

    private static final Logger logger = LoggerFactory.getLogger(PublicConfigurationTemplateInstanceManager.class);
    private final JmsService jmsService;
    private final SecurityService securityService;
    private final PublicResourceUtils publicResourceUtils;

    @Value("${catalogue.id}")
    private String catalogueId;

    public PublicConfigurationTemplateInstanceManager(JmsService jmsService, SecurityService securityService,
                                                      PublicResourceUtils publicResourceUtils) {
        super(ConfigurationTemplateInstanceBundle.class);
        this.jmsService = jmsService;
        this.securityService = securityService;
        this.publicResourceUtils = publicResourceUtils;
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
    @Override
    public ConfigurationTemplateInstanceBundle add(ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle, Authentication authentication) {
        String lowerLevelResourceId = configurationTemplateInstanceBundle.getId();
        Identifiers.createOriginalId(configurationTemplateInstanceBundle);
        configurationTemplateInstanceBundle.setId(publicResourceUtils.createPublicResourceId(
                configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getId(), catalogueId));

        // set public id to resourceId
        updateConfigurationTemplateInstanceIdsToPublic(configurationTemplateInstanceBundle);
        JSONParser parser = new JSONParser();
        try {
            JSONObject payload = (JSONObject) parser.parse(configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getPayload().replaceAll("'", "\""));
            payload.put("interoperabilityRecordId", publicResourceUtils.createPublicResourceId(
                    payload.get("interoperabilityRecordId").toString(), catalogueId));
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
        ConfigurationTemplateInstanceBundle published = super.get(publicResourceUtils.createPublicResourceId(
                configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getId(), catalogueId));
        ConfigurationTemplateInstanceBundle ret = super.get(publicResourceUtils.createPublicResourceId(
                configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getId(), catalogueId));
        try {
            BeanUtils.copyProperties(ret, configurationTemplateInstanceBundle);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        // set public id to resourceId
        updateConfigurationTemplateInstanceIdsToPublic(ret);
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
        try {
            ConfigurationTemplateInstanceBundle publicConfigurationTemplateInstanceBundle = get(publicResourceUtils.createPublicResourceId(
                    configurationTemplateInstanceBundle.getConfigurationTemplateInstance().getId(), catalogueId));
            logger.info(String.format("Deleting public ConfigurationTemplateInstanceBundle with id [%s]", publicConfigurationTemplateInstanceBundle.getId()));
            super.delete(publicConfigurationTemplateInstanceBundle);
            jmsService.convertAndSendTopic("configuration_template_instance.delete", publicConfigurationTemplateInstanceBundle);
        } catch (ResourceException | ResourceNotFoundException ignore) {
        }
    }

}
