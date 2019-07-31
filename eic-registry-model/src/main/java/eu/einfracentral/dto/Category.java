package eu.einfracentral.dto;

import eu.einfracentral.domain.Vocabulary;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
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

    public Vocabulary getSuperCategoryVocab() {
        return superCategoryVocab;
    }

    public void setSuperCategoryVocab(Vocabulary superCategoryVocab) {
        this.superCategoryVocab = superCategoryVocab;
    }

    public Vocabulary getCategoryVocab() {
        return categoryVocab;
    }

    public void setCategoryVocab(Vocabulary categoryVocab) {
        this.categoryVocab = categoryVocab;
    }

    public Vocabulary getSubCategoryVocab() {
        return subCategoryVocab;
    }

    public void setSubCategoryVocab(Vocabulary subCategoryVocab) {
        this.subCategoryVocab = subCategoryVocab;
    }
}
