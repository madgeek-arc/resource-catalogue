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

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceAlreadyExistsException;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.domain.AdminAuthentication;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.service.IdCreator;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VocabularyManager extends ResourceManager<Vocabulary> implements VocabularyService {
    private static final Logger logger = LoggerFactory.getLogger(VocabularyManager.class);

    private final OrganisationManager providerManager;

    public VocabularyManager(@Lazy OrganisationManager providerManager) {
        this.providerManager = providerManager;
    }

    public String getResourceTypeName() {
        return "vocabulary";
    }

    @Override
    public Vocabulary getOrElseThrow(String id) {
        Vocabulary vocabulary = null;
        try {
            vocabulary = get(id);
        } catch (ResourceException e) {
            throw new ResourceNotFoundException(id, "Vocabulary");
        }
        return vocabulary;
    }

    @Override
    public String[] getRegion(String name) {
        List<Vocabulary> allCountries = getByType(Vocabulary.Type.COUNTRY);
        if (name.equals("WW")) {
            return allCountries.stream().map(Vocabulary::getId).toArray(String[]::new);
        } else {
            return allCountries.stream()
                    .filter(vocabulary -> vocabulary.getExtras().containsKey("region"))
                    .filter(vocabulary -> vocabulary.getExtras().get("region").equals(name))
                    .map(Vocabulary::getId)
                    .toArray(String[]::new);
        }
    }

    @Override
    public Vocabulary getParent(String id) {
        return get(get(id).getParentId());
    }

    @Override
    public List<Vocabulary> getChildren(String parentId) {
        List<Vocabulary> children = new ArrayList<>();
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(getResourceTypeName());
        ff.setQuantity(maxQuantity);

        List<Vocabulary> allVocs = getAll(ff).getResults();
        for (Vocabulary vocabulary : allVocs) {
            if (parentId.equals(vocabulary.getParentId())) {
                children.add(vocabulary);
            }
        }

        children.sort(Comparator.comparing(Vocabulary::getName));
        return children;
    }

    @Override
    public List<Vocabulary> getByType(Vocabulary.Type type) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(getResourceTypeName());
        ff.setQuantity(maxQuantity);
        ff.addFilter("type", type.getKey());
        List<Vocabulary> vocList = getAll(ff, null).getResults();
        return vocList.stream().sorted(Comparator.comparing(Vocabulary::getName)).collect(Collectors.toList());
    }

    @Override
    public Map<Vocabulary.Type, List<Vocabulary>> getAllVocabulariesByType() {
        Map<Vocabulary.Type, List<Vocabulary>> allVocabularies = new HashMap<>();
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(getResourceTypeName());
        ff.setQuantity(maxQuantity);
        Paging<Vocabulary> allVocs = getAll(ff);
        allVocabularies = allVocs.getResults()
                .parallelStream()
                .filter(Objects::nonNull)
                .collect(Collectors
                        .groupingBy(value -> Vocabulary.Type.fromString(value.getType()),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> list.stream()
                                                .sorted(Comparator.comparing(Vocabulary::getName))
                                                .collect(Collectors.toList())
                                )
                        )
                );
        return allVocabularies;
    }

    @Override
    public void deleteAll(Authentication auth) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        List<Vocabulary> allVocs = getAll(ff, auth).getResults();
        for (Vocabulary vocabulary : allVocs) {
            delete(vocabulary);
        }
    }

    public void deleteByType(Vocabulary.Type type) {
        List<Vocabulary> toBeDeleted = getByType(type);
        for (Vocabulary vocabulary : toBeDeleted) {
            super.delete(vocabulary);
        }
    }

    @Override
    public Vocabulary add(Vocabulary vocabulary, Authentication auth) {
        if (vocabulary.getId() == null || "".equals(vocabulary.getId())) {
            String id = vocabulary.getName().toLowerCase();
            id = id.replace(" ", "_");
            id = id.replace("&", "and");
            if (vocabulary.getParentId() != null) {
                id = String.format("%s-%s", vocabulary.getParentId().toLowerCase(), id);
            }
            vocabulary.setId(id);
        }
        if (exists(vocabulary)) {
            throw new ResourceAlreadyExistsException(String.format("%s already exists!%n%s", getResourceTypeName(), vocabulary));
        }

        logger.debug("Adding Vocabulary {}", vocabulary);
        super.add(vocabulary, auth);

        return vocabulary;
    }

    @Scheduled(cron = "0 0 12 ? * 2/7") // At 12:00pm, every 7 days starting on Monday, every month
    //    @Scheduled(initialDelay = 0, fixedRate = 120000)
    public void updateHostingLegalEntityVocabularyList() {
        logger.info("Checking for possible new Hosting Legal Entity entries..");
        List<Vocabulary> hostingLegalEntities = getByType(Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY);
        List<String> hostingLegalEntityNames = new ArrayList<>();
        for (Vocabulary hostingLegalEntity : hostingLegalEntities) {
            hostingLegalEntityNames.add(hostingLegalEntity.getName());
        }
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.addFilter("active", true);
        ff.addFilter("status", "approved");
        ff.addFilter("published", false);
        List<OrganisationBundle> allActiveAndApprovedProviders = providerManager.getAll(ff, new AdminAuthentication()).getResults();
        Map<String, String> providerNames = new LinkedHashMap<>();
        for (OrganisationBundle organisationBundle : allActiveAndApprovedProviders) {
            if ((boolean) organisationBundle.getOrganisation().get("legalEntity")) {
                providerNames.put(
                        (String) organisationBundle.getOrganisation().get("name"),
                        organisationBundle.getCatalogueId()
                );
            }
        }
        for (Iterator<String> it = providerNames.keySet().iterator(); it.hasNext(); ) {
            String providerName = it.next();
            for (String hleName : hostingLegalEntityNames) {
                if (hleName.contains(providerName)) {
                    it.remove();
                    break;
                }
            }
        }
        updateHLEVocabularyList(providerNames);
    }

    private void updateHLEVocabularyList(Map<String, String> providerNames) {
        for (Map.Entry<String, String> entry : providerNames.entrySet()) {
            Vocabulary newHostingLegalEntity = new Vocabulary();
            newHostingLegalEntity.setId(idCreator.sanitizeString(entry.getKey()));
            newHostingLegalEntity.setName(entry.getKey());
            newHostingLegalEntity.setType(Vocabulary.Type.PROVIDER_HOSTING_LEGAL_ENTITY.getKey());
            if (entry.getValue() != null) {
                newHostingLegalEntity.setExtras(Map.of("catalogueId", entry.getValue()));
            }
            logger.info("Creating a new Hosting Legal Entity Vocabulary with id: '{}' and name: '{}'",
                    newHostingLegalEntity.getId(), newHostingLegalEntity.getName());
            add(newHostingLegalEntity, null);
        }
    }
}
