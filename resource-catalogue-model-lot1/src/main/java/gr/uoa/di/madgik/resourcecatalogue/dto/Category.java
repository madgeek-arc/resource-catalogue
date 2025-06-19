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

package gr.uoa.di.madgik.resourcecatalogue.dto;

import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import jakarta.xml.bind.annotation.XmlTransient;

@XmlTransient
public class Category {

    private Vocabulary superCategoryVocab;
    private Vocabulary categoryVocab;
    private Vocabulary subCategoryVocab;

    public Category() {
    }

    public Category(Vocabulary superCategoryVocab, Vocabulary category, Vocabulary subCategoryVocab) {
        this.superCategoryVocab = superCategoryVocab;
        this.categoryVocab = category;
        this.subCategoryVocab = subCategoryVocab;
    }

    public Vocabulary getSuperCategory() {
        return superCategoryVocab;
    }

    public void setSuperCategory(Vocabulary superCategory) {
        this.superCategoryVocab = superCategory;
    }

    public Vocabulary getCategory() {
        return categoryVocab;
    }

    public void setCategory(Vocabulary category) {
        this.categoryVocab = category;
    }

    public Vocabulary getSubCategory() {
        return subCategoryVocab;
    }

    public void setSubCategory(Vocabulary subCategory) {
        this.subCategoryVocab = subCategory;
    }
}
