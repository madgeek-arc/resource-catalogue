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

package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;

import java.net.URL;

public class EOSCIFGuidelines {

    private String pid;

    private String label;

    @FieldValidation(nullable = true)
    private URL url;

    @FieldValidation(nullable = true, containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.SEMANTIC_RELATIONSHIP)
    private String semanticRelationship;

    public EOSCIFGuidelines() {
    }

    public EOSCIFGuidelines(String pid, String label, URL url, String semanticRelationship) {
        this.pid = pid;
        this.label = label;
        this.url = url;
        this.semanticRelationship = semanticRelationship;
    }

    @Override
    public String toString() {
        return "EOSCIFGuidelines{" +
                "pid='" + pid + '\'' +
                ", label='" + label + '\'' +
                ", url=" + url +
                ", semanticRelationship='" + semanticRelationship + '\'' +
                '}';
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getSemanticRelationship() {
        return semanticRelationship;
    }

    public void setSemanticRelationship(String semanticRelationship) {
        this.semanticRelationship = semanticRelationship;
    }
}
