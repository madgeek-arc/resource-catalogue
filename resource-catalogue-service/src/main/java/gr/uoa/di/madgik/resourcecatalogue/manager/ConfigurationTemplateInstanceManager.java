/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ConfigurationTemplateBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ConfigurationTemplateInstanceBundle;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.ProviderResourcesCommonMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

@org.springframework.stereotype.Service("configurationTemplateInstanceManager")
public class ConfigurationTemplateInstanceManager extends ResourceCatalogueGenericManager<ConfigurationTemplateInstanceBundle>
        implements ConfigurationTemplateInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationTemplateInstanceManager.class);
    private final ConfigurationTemplateInstanceService service;
    private final ResourceInteroperabilityRecordService rirService;

    private final ConfigurationTemplateService configService;
    private final SecurityService securityService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final GenericResourceService genericResourceService;
    private final VocabularyService vocabularyService;

    @Value("${catalogue.id}")
    private String catalogueId;

    public ConfigurationTemplateInstanceManager(@Lazy ConfigurationTemplateInstanceService service,
                                                @Lazy ConfigurationTemplateService configService,
                                                @Lazy ResourceInteroperabilityRecordService rirService,
                                                SecurityService securityService, IdCreator idCreator,
                                                ProviderResourcesCommonMethods commonMethods,
                                                GenericResourceService genericResourceService,
                                                VocabularyService vocabularyService) {
        super(genericResourceService, idCreator, securityService, vocabularyService);
        this.service = service;
        this.configService = configService;
        this.rirService = rirService;
        this.securityService = securityService;
        this.commonMethods = commonMethods;
        this.genericResourceService = genericResourceService;
        this.vocabularyService = vocabularyService;
    }

    public String getResourceTypeName() {
        return "configuration_template_instance";
    }

    @Override
    public ConfigurationTemplateInstanceBundle add(ConfigurationTemplateInstanceBundle cti, Authentication auth) {
        checkResourceIdAndConfigurationTemplateIdConsistency(cti, auth);
        validateInstanceAgainstTemplate(cti);

        cti.markOnboard(vocabularyService.get("approved").getId(), true, UserInfo.of(auth), null);
        cti.setActive(true);
        cti.setCatalogueId(this.catalogueId);
        this.createIdentifiers(cti, getResourceTypeName(), false);
        cti.setId(cti.getIdentifiers().getOriginalId());
        ConfigurationTemplateInstanceBundle ret = genericResourceService.add(getResourceTypeName(), cti);
        return ret;
    }

    @Override
    public ConfigurationTemplateInstanceBundle update(ConfigurationTemplateInstanceBundle bundle, Authentication auth) {
        ConfigurationTemplateInstanceBundle existing = get(bundle.getId(), bundle.getCatalogueId());
        // check if there are actual changes in the Service
        if (bundle.equals(existing)) {
            return bundle;
        }
        bundle.markUpdate(UserInfo.of(auth), null);

        // block user from updating resourceId
        if (!bundle.getConfigurationTemplateInstance().get("resourceId").equals(existing.getConfigurationTemplateInstance().get("resourceId")) &&
                !securityService.hasRole(auth, "ROLE_ADMIN")) {
            throw new ValidationException("You cannot change the Resource Id with which this " +
                    "ConfigurationTemplateInstance is related");
        }
        // block user from updating configurationTemplateId
        if (!bundle.getConfigurationTemplateInstance().get("configurationTemplateId")
                .equals(existing.getConfigurationTemplateInstance().get("configurationTemplateId")) &&
                !securityService.hasRole(auth, "ROLE_ADMIN")) {
            throw new ValidationException("You cannot change the Configuration Template Id with which this " +
                    "ConfigurationTemplateInstance is related");
        }

        try {
            return genericResourceService.update(getResourceTypeName(), bundle.getId(), bundle);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(ConfigurationTemplateInstanceBundle bundle) {
        commonMethods.blockResourceDeletion(bundle.getStatus(), bundle.getMetadata().isPublished());
        logger.info("Deleting Configuration Template Instance: {}", bundle.getId());
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    public List<ConfigurationTemplateInstanceBundle> getByResourceId(String id) {
        List<ConfigurationTemplateInstanceBundle> ret = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        List<ConfigurationTemplateInstanceBundle> list = service.getAll(ff, null).getResults();
        for (ConfigurationTemplateInstanceBundle bundle : list) {
            if (bundle.getConfigurationTemplateInstance().get("resourceId").equals(id)) {
                ret.add(bundle);
            }
        }
        return ret;
    }

    public List<LinkedHashMap<String, Object>> getByConfigurationTemplateId(String id) {
        List<LinkedHashMap<String, Object>> ret = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", false);
        List<ConfigurationTemplateInstanceBundle> list = service.getAll(ff, null).getResults();
        for (ConfigurationTemplateInstanceBundle bundle : list) {
            if (bundle.getConfigurationTemplateInstance().get("configurationTemplateId").equals(id)) {
                ret.add(bundle.getConfigurationTemplateInstance());
            }
        }
        return ret;
    }

    public LinkedHashMap<String, Object> getByResourceAndConfigurationTemplateId(String resourceId, String ctId) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", false);
        ff.addFilter("resource_id", resourceId);
        ff.addFilter("configuration_template_id", ctId);
        List<ConfigurationTemplateInstanceBundle> list = service.getAll(ff, null).getResults();
        if (!list.isEmpty()) {
            return list.getFirst().getConfigurationTemplateInstance();
        }
        return null;
    }

    private void checkResourceIdAndConfigurationTemplateIdConsistency(ConfigurationTemplateInstanceBundle bundle,
                                                                      Authentication auth) {
        String resourceId = (String) bundle.getConfigurationTemplateInstance().get("resourceId");
        String configurationTemplateId = (String) bundle.getConfigurationTemplateInstance().get("configurationTemplateId");
        // check if the configuration template ID is related to the resource ID
        boolean found = false;
        List<ResourceInteroperabilityRecordBundle> list = rirService.getAll(createFacetFilter(), auth).getResults();
        for (ResourceInteroperabilityRecordBundle rirBundle : list) {
            if (rirBundle.getResourceInteroperabilityRecord().get("resourceId").equals(resourceId)) {
                ConfigurationTemplateBundle ctBundle = configService.get(configurationTemplateId);
                Collection<String> rirIds = (Collection<String>) rirBundle.getResourceInteroperabilityRecord()
                        .get("interoperabilityRecordIds");
                String ctInteroperabilityId = (String) ctBundle.getConfigurationTemplate()
                        .get("interoperabilityRecordId");
                if (rirIds.contains(ctInteroperabilityId)) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            throw new ValidationException("Fields resourceId and configurationTemplateId are not related.");
        }

        // check if a Configuration Template Implementation with the same resourceId, configurationTemplateId already exists
        List<ConfigurationTemplateInstanceBundle> configurationTemplateInstanceBundleList = service.getAll(createFacetFilter(), auth).getResults();
        for (ConfigurationTemplateInstanceBundle ctiBundle : configurationTemplateInstanceBundleList) {
            if (ctiBundle.getConfigurationTemplateInstance().get("resourceId").equals(resourceId) &&
                    ctiBundle.getConfigurationTemplateInstance().get("configurationTemplateId").equals(configurationTemplateId)) {
                throw new ValidationException(String.format("There is already a Configuration Template Instance registered " +
                                "for Resource [%s] under [%s] Configuration Template",
                        resourceId, configurationTemplateId));
            }
        }
    }

    private FacetFilter createFacetFilter() {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        ff.addFilter("published", false);
        return ff;
    }

    private void validateInstanceAgainstTemplate(ConfigurationTemplateInstanceBundle cti) {
        ConfigurationTemplateBundle ct = configService.get((String) cti.getConfigurationTemplateInstance().get("configurationTemplateId"),
                cti.getCatalogueId());
        Set<String> ctKeys = ((Map<String, Object>) ct.getConfigurationTemplate().get("formModel")).keySet();
        Set<String> ctiKeys = ((Map<String, Object>) cti.getConfigurationTemplateInstance().get("payload")).keySet();

        if (!ctKeys.containsAll(ctiKeys)) {
            throw new ValidationException("Configuration Template Instance does not contain the required model in its payload");
        }
    }

    //region Not-Used
    @Override
    public Browsing<ConfigurationTemplateInstanceBundle> getMy(FacetFilter filter, Authentication authentication) {
        return null;
    }

    @Override
    public ConfigurationTemplateInstanceBundle update(ConfigurationTemplateInstanceBundle bundle, String comment, Authentication auth) {
        return null;
    }

    @Override
    public ConfigurationTemplateInstanceBundle verify(String id, String status, Boolean active, Authentication auth) {
        return null;
    }

    @Override
    public ConfigurationTemplateInstanceBundle setActive(String id, Boolean active, Authentication auth) {
        return null;
    }

    @Override
    public ConfigurationTemplateInstanceBundle addDraft(ConfigurationTemplateInstanceBundle bundle, Authentication auth) {
        return null;
    }

    @Override
    public ConfigurationTemplateInstanceBundle updateDraft(ConfigurationTemplateInstanceBundle bundle, Authentication auth) {
        return null;
    }

    @Override
    public void deleteDraft(ConfigurationTemplateInstanceBundle bundle) {

    }

    @Override
    public ConfigurationTemplateInstanceBundle finalizeDraft(ConfigurationTemplateInstanceBundle configurationTemplateInstanceBundle, Authentication auth) {
        return null;
    }

    @Override
    public void addBulk(List<ConfigurationTemplateInstanceBundle> resources, Authentication auth) {
        super.addBulk(resources, auth);
    }

    @Override
    public void updateBulk(List<ConfigurationTemplateInstanceBundle> resources, Authentication auth) {
        super.updateBulk(resources, auth);
    }
    //endregion
}
