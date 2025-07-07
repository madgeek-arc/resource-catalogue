/**
 *Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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

package gr.uoa.di.madgik.resourcecatalogue.domain.configurationTemplates;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiable;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.json.simple.JSONObject;

@XmlType
@XmlRootElement
public class ConfigurationTemplate implements Identifiable {

    @XmlElement
    @Schema(example = "(required on PUT only)")
    @FieldValidation
    private String id;

    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation(containsId = true, idClass = InteroperabilityRecord.class)
    private String interoperabilityRecordId;

    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String name;

    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private String description;

    @XmlElement(required = true)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FieldValidation
    private JSONObject formModel;

    public ConfigurationTemplate() {
    }

    public ConfigurationTemplate(String id, String interoperabilityRecordId, String name, String description,
                                 JSONObject formModel) {
        this.id = id;
        this.interoperabilityRecordId = interoperabilityRecordId;
        this.name = name;
        this.description = description;
        this.formModel = formModel;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getInteroperabilityRecordId() {
        return interoperabilityRecordId;
    }

    public void setInteroperabilityRecordId(String interoperabilityRecordId) {
        this.interoperabilityRecordId = interoperabilityRecordId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JSONObject getFormModel() {
        return formModel;
    }

    public void setFormModel(JSONObject formModel) {
        this.formModel = formModel;
    }
}
