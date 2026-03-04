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

package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.domain.deprecated.Adapter;
import gr.uoa.di.madgik.resourcecatalogue.domain.deprecated.Catalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.deprecated.Provider;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceInteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.*;

@Deprecated
@Component
public class ProviderResourcesCommonMethods {

    private static final Logger logger = LoggerFactory.getLogger(ProviderResourcesCommonMethods.class);

    @Value("${elastic.index.max_result_window:10000}")
    protected int maxQuantity;

    private final VocabularyService vocabularyService;
    private final ResourceInteroperabilityRecordService resourceInteroperabilityRecordService;

    public ProviderResourcesCommonMethods(@Lazy VocabularyService vocabularyService,
                                          @Lazy ResourceInteroperabilityRecordService resourceInteroperabilityRecordService) {
        this.vocabularyService = vocabularyService;
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
    }

    public void blockResourceDeletion(String status, boolean isPublished) {
        if (status.equals(vocabularyService.get("pending").getId())) {
            throw new ResourceException("You cannot delete a Template that is under review", HttpStatus.FORBIDDEN);
        }
        if (isPublished) {
            throw new ResourceException("You cannot directly delete a Public Resource", HttpStatus.FORBIDDEN);
        }
    }

    public void deleteResourceInteroperabilityRecords(String resourceId, String resourceType) {
        ResourceInteroperabilityRecordBundle resourceInteroperabilityRecordBundle =
                resourceInteroperabilityRecordService.getByResourceId(resourceId);
        if (resourceInteroperabilityRecordBundle != null) {
            try {
                logger.info("Deleting ResourceInteroperabilityRecord of {} with id: '{}'", resourceType, resourceId);
                resourceInteroperabilityRecordService.delete(resourceInteroperabilityRecordBundle);
            } catch (ResourceNotFoundException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public void addAuthenticatedUser(Object object, Authentication auth) {
        User authUser = User.of(auth);
        if (object instanceof LinkedHashMap<?, ?> raw) {
            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> payload = (LinkedHashMap<String, Object>) raw;

            Object value = payload.get("users");
            Set<User> users = new LinkedHashSet<>();
            if (value instanceof Collection<?> collection) {
                for (Object o : collection) {
                    if (o instanceof User user) {
                        users.add(user);
                    } else if (o instanceof Map<?, ?> map) {
                        users.add(fromMap(map));
                    }
                }
            }
            users.add(authUser);
            payload.put("users", new ArrayList<>(users));
        } else if (object instanceof Catalogue catalogue) {
            Set<User> users = catalogue.getUsers() == null ? new HashSet<>() : new HashSet<>(catalogue.getUsers());
            users.add(authUser);
            catalogue.setUsers(new ArrayList<>(users));
        } else if (object instanceof Provider provider) {
            Set<User> users = provider.getUsers() == null ? new HashSet<>() : new HashSet<>(provider.getUsers());
            users.add(authUser);
            provider.setUsers(new ArrayList<>(users));
        } else if (object instanceof Adapter adapter) {
            Set<User> users = adapter.getAdmins() == null ? new HashSet<>() : new HashSet<>(adapter.getAdmins());
            users.add(authUser);
            adapter.setAdmins(new ArrayList<>(users));
        }
    }

    private static User fromMap(Map<?, ?> map) {
        User user = new User();
        user.setId((String) map.get("id"));
        user.setName((String) map.get("name"));
        user.setSurname((String) map.get("surname"));
        user.setEmail((String) map.get("email"));
        return user;
    }
}