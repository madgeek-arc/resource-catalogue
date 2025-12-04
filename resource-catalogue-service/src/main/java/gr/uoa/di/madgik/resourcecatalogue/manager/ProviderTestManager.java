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

import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.dto.CatalogueValue;
import gr.uoa.di.madgik.resourcecatalogue.dto.MapValues;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import java.util.*;

@org.springframework.stereotype.Service("providerTestManager")
public class ProviderTestManager implements ProviderTestService {

    private static final Logger logger = LoggerFactory.getLogger(ProviderTestManager.class);
    private final String resourceTypeName = "providertest";

    private final GenericResourceService genericResourceService;

    public ProviderTestManager(GenericResourceService genericResourceService) {
        this.genericResourceService = genericResourceService;
    }

    @Override
    public void requestProviderDeletion(FacetFilter ff, Authentication auth) {
        //TODO: fill method
    }

    @Override
    public NewProviderBundle createPublicProvider(NewProviderBundle bundle, Authentication auth) {
        //TODO: fill method
        return null;
    }

    @Override
    public String determineHostingLegalEntity(String providerName) {
        //TODO: fill method
        return "";
    }

    @Override
    public List<MapValues<CatalogueValue>> getAllResourcesUnderASpecificHLE(String hle, Authentication auth) {
        //TODO: fill method
        return List.of();
    }

    @Override
    public NewProviderBundle get(String id) {
        //TODO: fill method
        return null;
    }

    @Override
    public Browsing<NewProviderBundle> getAll(FacetFilter filter, Authentication authentication) {
        //TODO: fill method
        return null;
    }

    @Override
    public Browsing<NewProviderBundle> getMy(FacetFilter filter, Authentication authentication) {
        //TODO: fill method
        return null;
    }

    @Override
    public NewProviderBundle add(NewProviderBundle resource, Authentication authentication) {
        //TODO: fill method
        return null;
    }

    @Override
    public NewProviderBundle update(NewProviderBundle resource, Authentication authentication) {
        //TODO: fill method
        return null;
    }

    @Override
    public void delete(NewProviderBundle resourceId) {
        //TODO: fill method
    }

    @Override
    public NewProviderBundle verify(String id, String status, Boolean active, Authentication auth) {
        //TODO: fill method
        return null;
    }

    @Override
    public NewProviderBundle publish(String id, Boolean active, Authentication auth) {
        //TODO: fill method
        return null;
    }

    @Override
    public boolean hasAdminAcceptedTerms(FacetFilter ff, Authentication auth) {
        //TODO: fill method
        return false;
    }

    @Override
    public void adminAcceptedTerms(FacetFilter ff, Authentication auth) {
        //TODO: fill method
    }

    @Override
    public NewProviderBundle suspend(String id, String catalogueId, boolean suspend, Authentication auth) {
        //TODO: fill method
        return null;
    }

    @Override
    public NewProviderBundle audit(String id, String catalogueId, String comment, LoggingInfo.ActionType actionType, Authentication auth) {
        //TODO: fill method
        return null;
    }

    @Override
    public Paging<NewProviderBundle> getRandomResourcesForAuditing(int quantity, int auditingInterval, Authentication auth) {
        //TODO: fill method
        return null;
    }

    @Override
    public Browsing<NewProviderBundle> getAll(FacetFilter filter) {
        //TODO: fill method
        return null;
    }

    @Override
    public NewProviderBundle get(String id, String catalogueId) {
        //TODO: fill method
        return null;
    }

    @Override
    public String createId(NewProviderBundle newProviderBundle) {
        //TODO: fill method
        return "";
    }

    @Override
    public NewProviderBundle save(NewProviderBundle newProviderBundle) {
        //TODO: fill method
        return null;
    }

    @Override
    public Map<String, List<NewProviderBundle>> getBy(String field) {
        //TODO: fill method
        return Map.of();
    }

    @Override
    public List<NewProviderBundle> getSome(String... ids) {
        //TODO: fill method
        return List.of();
    }

    @Override
    public NewProviderBundle get(SearchService.KeyValue... keyValues) {
        //TODO: fill method
        return null;
    }

    @Override
    public List<NewProviderBundle> delAll() {
        //TODO: fill method
        return List.of();
    }

    @Override
    public NewProviderBundle validate(NewProviderBundle newProviderBundle) {
        //TODO: fill method
        return null;
    }

    @Override
    public Resource getResource(String id) {
        //TODO: fill method
        return null;
    }

    @Override
    public Resource getResource(String id, String catalogueId) {
        //TODO: fill method
        return null;
    }

    @Override
    public boolean exists(NewProviderBundle newProviderBundle) {
        //TODO: fill method
        return false;
    }

    @Override
    public boolean exists(String id) {
        //TODO: fill method
        return false;
    }
}
