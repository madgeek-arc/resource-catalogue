package gr.uoa.di.madgik.resourcecatalogue.dto;

import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;

import javax.xml.bind.annotation.XmlTransient;

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
