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

package gr.uoa.di.madgik.resourcecatalogue.service;

import com.google.gson.JsonArray;
import gr.uoa.di.madgik.resourcecatalogue.domain.MonitoringBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.dto.MonitoringStatus;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface MonitoringService extends ResourceCatalogueService<MonitoringBundle> {

    /**
     * Creates a new Monitoring
     *
     * @param monitoring   MonitoringBundle
     * @param resourceType String Resource Type
     * @param auth         Authentication
     * @return {@link MonitoringBundle}
     */
    MonitoringBundle add(MonitoringBundle monitoring, String resourceType, Authentication auth);

    /**
     * Returns all the available Service Types
     *
     * @return {@link List<Vocabulary>}
     */
    List<Vocabulary> getAvailableServiceTypes();

    /**
     * Retrieve {@link MonitoringBundle} for a catalogue specific resource.
     *
     * @param serviceId   String Service ID
     * @param catalogueId String Catalogue ID
     * @return {@link MonitoringBundle}
     */
    MonitoringBundle get(String serviceId, String catalogueId);

    /**
     * Validates the given Monitoring
     *
     * @param monitoringBundle MonitoringBundle
     * @param resourceType     String Resource Type
     * @return {@link MonitoringBundle}
     */
    MonitoringBundle validate(MonitoringBundle monitoringBundle, String resourceType);

    /**
     * @param monitoringBundle MonitoringBundle
     * @param auth             Authentication
     */
    void updateBundle(MonitoringBundle monitoringBundle, Authentication auth);

    /**
     * Creates a Public version of the specific Monitoring
     *
     * @param monitoringBundle MonitoringBundle
     * @param auth             Authentication
     * @return {@link MonitoringBundle}
     */
    MonitoringBundle createPublicResource(MonitoringBundle monitoringBundle, Authentication auth);


    // Argo GRNET Monitoring Status methods

    /**
     * Returns a list of Monitoring's Availability Object
     *
     * @param results JsonArray Results
     * @return {@link List<MonitoringStatus>}
     */
    List<MonitoringStatus> createMonitoringAvailabilityObject(JsonArray results);

    /**
     * Returns a list of Monitoring's Status Objects
     *
     * @param results JsonArray Results
     * @return {@link List<MonitoringStatus>}
     */
    List<MonitoringStatus> createMonitoringStatusObject(JsonArray results);
}
