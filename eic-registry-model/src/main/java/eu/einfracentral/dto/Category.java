package eu.einfracentral.dto;

import eu.einfracentral.domain.Vocabulary;

public class Category {
    Vocabulary superCategory;
    Vocabulary category;
    Vocabulary subCategory;

    public Category() {
    }

    public Category(Vocabulary superCategory, Vocabulary category, Vocabulary subCategory) {
        this.superCategory = superCategory;
        this.category = category;
        this.subCategory = subCategory;
    }

    public Vocabulary getSuperCategory() {
        return superCategory;
    }

    public void setSuperCategory(Vocabulary superCategory) {
        this.superCategory = superCategory;
    }

    public Vocabulary getCategory() {
        return category;
    }

    public void setCategory(Vocabulary category) {
        this.category = category;
    }

    public Vocabulary getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(Vocabulary subCategory) {
        this.subCategory = subCategory;
    }
}
