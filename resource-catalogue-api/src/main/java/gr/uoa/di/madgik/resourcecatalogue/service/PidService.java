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

package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;

import java.util.List;

public interface PidService {

    /**
     * Get a Resource via its PID
     *
     * @param prefix PID prefix
     * @param suffix PID suffix
     * @return Bundle<?>
     */
    Bundle get(String prefix, String suffix);

    /**
     * Registers/Updates a PID on a specific resource
     *
     * @param bundle    the resource whose PID record is being registered/updated
     * @param endpoints Extra resolve endpoints to append on top of the resource's own URLs (optional)
     */
    void register(Bundle bundle, List<String> endpoints);

    /**
     * Deletes the resource from the PID service
     *
     * @param pid       PID
     */
    void delete(String pid);
}