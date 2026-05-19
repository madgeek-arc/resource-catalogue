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

package gr.uoa.di.madgik.resourcecatalogue.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ResourceCatalogueInfo {

    @Value("${catalogue.emails.support}")
    private String catalogueSupportEmail;
    @Value("${node.pid}")
    private String nodePid;
    @Value("${node.pid.allow-vocabulary-values}")
    private boolean nodePidAllowVocabularyValues;

    public ResourceCatalogueInfo() {
    }

    public ResourceCatalogueInfo(String catalogueSupportEmail, String nodePid, boolean nodePidAllowVocabularyValues) {
        this.catalogueSupportEmail = catalogueSupportEmail;
        this.nodePid = nodePid;
        this.nodePidAllowVocabularyValues = nodePidAllowVocabularyValues;
    }

    public String getCatalogueSupportEmail() {
        return catalogueSupportEmail;
    }

    public void setCatalogueSupportEmail(String catalogueSupportEmail) {
        this.catalogueSupportEmail = catalogueSupportEmail;
    }

    public String getNodePid() {
        return nodePid;
    }

    public void setNodePid(String nodePid) {
        this.nodePid = nodePid;
    }

    public boolean allowsVocabularyValues() {
        return nodePidAllowVocabularyValues;
    }

    public void setNodePidAllowVocabularyValues(boolean nodePidAllowVocabularyValues) {
        this.nodePidAllowVocabularyValues = nodePidAllowVocabularyValues;
    }
}
