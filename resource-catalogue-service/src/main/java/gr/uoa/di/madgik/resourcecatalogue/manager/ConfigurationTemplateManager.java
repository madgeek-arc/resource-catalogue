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

import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.domain.ConfigurationTemplateBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import gr.uoa.di.madgik.resourcecatalogue.onboarding.WorkflowService;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

@org.springframework.stereotype.Service("configurationTemplateManager")
public class ConfigurationTemplateManager extends ResourceCatalogueGenericManager<ConfigurationTemplateBundle>
        implements ConfigurationTemplateService {

    private static final Logger logger = LogManager.getLogger(ConfigurationTemplateManager.class);
    private final OrganisationService organisationService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final GenericResourceService genericResourceService;
    private final VocabularyService vocabularyService;
    private final WebClient webClient;

    @Value("${argo.grnet.monitoring.token:}")
    private String monitoringToken;
    @Value("${argo.grnet.monitoring.service.types:}")
    private String monitoringServiceTypes;

    public ConfigurationTemplateManager(IdCreator idCreator,
                                        OrganisationService organisationService,
                                        InteroperabilityRecordService interoperabilityRecordService,
                                        SecurityService securityService,
                                        GenericResourceService genericResourceService,
                                        VocabularyService vocabularyService,
                                        WebClient.Builder webClientBuilder,
                                        WorkflowService workflowService) {
        super(genericResourceService, idCreator, securityService, vocabularyService, workflowService);
        this.organisationService = organisationService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.vocabularyService = vocabularyService;
        this.genericResourceService = genericResourceService;
        this.webClient = webClientBuilder.build();
    }

    public String getResourceTypeName() {
        return "configuration_template";
    }

    @Override
    public ConfigurationTemplateBundle add(ConfigurationTemplateBundle ct, Authentication auth) {
        InteroperabilityRecordBundle interoperabilityRecordBundle = interoperabilityRecordService.get(
                (String) ct.getConfigurationTemplate().get("interoperabilityRecordId"));
        OrganisationBundle organisationBundle = organisationService.get(
                (String) interoperabilityRecordBundle.getInteroperabilityRecord().get("resourceOwner"),
                interoperabilityRecordBundle.getCatalogueId());
        if (!organisationBundle.getStatus().equals("approved")) {
            throw new ResourceException(String.format("The Provider ID '%s' you provided is not yet approved",
                    organisationBundle.getId()), HttpStatus.CONFLICT);
        }

        ct.markOnboard(vocabularyService.get("approved").getId(), true, UserInfo.of(auth), null);
        ct.setActive(true);
        ct.setCatalogueId(null);
        this.createIdentifiers(ct, getResourceTypeName(), false);
        ct.setId(ct.getIdentifiers().getOriginalId());
        ConfigurationTemplateBundle ret = genericResourceService.add(getResourceTypeName(), ct, false); //FIXME
        return ret;
    }

    @Override
    public ConfigurationTemplateBundle update(ConfigurationTemplateBundle bundle, String comment, Authentication auth) {
        ConfigurationTemplateBundle existing = get(bundle.getId(), bundle.getCatalogueId());
        // check if there are actual changes in the Service
        if (bundle.equals(existing)) {
            return bundle;
        }
        bundle.markUpdate(UserInfo.of(auth), comment);

        try {
            return genericResourceService.update(getResourceTypeName(), bundle.getId(), bundle);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(ConfigurationTemplateBundle bundle) {
        blockResourceDeletion(bundle.getStatus(), bundle.getMetadata().isPublished());
        logger.info("Deleting Configuration Template: {}", bundle.getId());
        genericResourceService.delete(getResourceTypeName(), bundle.getId());
    }

    @Override
    public Paging<ConfigurationTemplateBundle> getAllByInteroperabilityRecordId(MultiValueMap<String, Object> params,
                                                                                String interoperabilityRecordId) {
        FacetFilter ff;
        if (params != null) {
            ff = FacetFilter.from(params);
        } else {
            ff = new FacetFilter();
        }
        ff.setResourceType("configuration_template");
        ff.setQuantity(1000);
        ff.addFilter("published", false);
        ff.addFilter("interoperability_record_id", interoperabilityRecordId);
        return genericResourceService.getResults(ff);
    }

    public Map<String, List<String>> getInteroperabilityRecordIdToConfigurationTemplateListMap() {
        Map<String, List<String>> ret = new HashMap<>();
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(getResourceTypeName());
        filter.setQuantity(1000);
        filter.addFilter("published", false);
        List<ConfigurationTemplateBundle> ctList = getAll(filter).getResults();

        for (ConfigurationTemplateBundle ctBundle : ctList) {
            String igId = (String) ctBundle.getConfigurationTemplate().get("interoperabilityRecordId");
            String ctId = ctBundle.getId();

            ret.computeIfAbsent(igId, k -> new ArrayList<>()).add(ctId);
        }
        return ret;
    }

    // Old Monitoring Service Types
    public List<Vocabulary> getAvailableServiceTypes() {
        String response = callMonitoringApi(monitoringServiceTypes, monitoringToken);
        if (response == null || response.isEmpty()) return Collections.emptyList();

        JSONObject obj = new JSONObject(response);
        JSONArray array = obj.getJSONArray("data");
        return createServiceTypeVocabularyList(array);
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

    //region Not-Needed
    @Override
    public Browsing<ConfigurationTemplateBundle> getMy(FacetFilter filter, Authentication authentication) {
        return null;
    }

    @Override
    public ConfigurationTemplateBundle verify(String id, String status, Boolean active, Authentication auth) {
        return null;
    }

    @Override
    public ConfigurationTemplateBundle setActive(String id, Boolean active, Authentication auth) {
        return null;
    }

    @Override
    public ConfigurationTemplateBundle addDraft(ConfigurationTemplateBundle bundle, Authentication auth) {
        return null;
    }

    @Override
    public ConfigurationTemplateBundle updateDraft(ConfigurationTemplateBundle bundle, Authentication auth) {
        return null;
    }

    @Override
    public void deleteDraft(ConfigurationTemplateBundle bundle) {

    }

    @Override
    public ConfigurationTemplateBundle finalizeDraft(ConfigurationTemplateBundle configurationTemplateBundle, Authentication auth) {
        return null;
    }

    @Override
    public void addBulk(List<ConfigurationTemplateBundle> resources, Authentication auth) {
        super.addBulk(resources, auth);
    }

    @Override
    public void updateBulk(List<ConfigurationTemplateBundle> resources, Authentication auth) {
        super.updateBulk(resources, auth);
    }
    //endregion
}
