/**
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

package gr.uoa.di.madgik.resourcecatalogue.utils;

public class PublicResourceUtils {

    //TODO: remove me
    /**
     * Creates public ID for the following resources:
     * Provider, Service, Datasource, Helpdesk, Monitoring, Training Resource, Interoperability Record
     * Configuration Template Instance, Resource Interoperability Record
     *
     * @param id          resource ID
     * @param catalogueId resource catalogue ID
     * @return public id
     */
    public static String createPublicResourceId(String id, String catalogueId) {
        String[] parts = id.split("/");
        String prefix = parts[0];
        String suffix = parts[1];
        return prefix + "/" + catalogueId + "." + suffix;
    }
}
