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

package gr.uoa.di.madgik.resourcecatalogue.domain;


import org.json.simple.JSONObject;

public class ConfigurationTemplateInstanceDto {

    private String id;
    private String resourceId;
    private String configurationTemplateId;
    private JSONObject payload;

    public ConfigurationTemplateInstanceDto() {
    }

    public ConfigurationTemplateInstanceDto(String id, String resourceId, String configurationTemplateId, JSONObject payload) {
        this.id = id;
        this.resourceId = resourceId;
        this.configurationTemplateId = configurationTemplateId;
        this.payload = payload;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getConfigurationTemplateId() {
        return configurationTemplateId;
    }

    public void setConfigurationTemplateId(String configurationTemplateId) {
        this.configurationTemplateId = configurationTemplateId;
    }

    public JSONObject getPayload() {
        return payload;
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }
}
