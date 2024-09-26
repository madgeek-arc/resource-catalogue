package gr.uoa.di.madgik.resourcecatalogue.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class ServiceCategory {


    // Provider's Location Information
    /**
     * A named group of Resources that offer access to the same type of Resources
     */
    @XmlElement(required = true)
    @Schema
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.CATEGORY)
    private String category;

    /**
     * A named group of Resources that offer access to the same type of Resource or capabilities, within the defined Resource Category.
     */
    @XmlElement(required = true)
    @Schema
    @FieldValidation(containsId = true, idClass = Vocabulary.class)
    @VocabularyValidation(type = Vocabulary.Type.SUBCATEGORY)
    private String subcategory;

    public ServiceCategory() {
    }

    public ServiceCategory(String category, String subcategory) {
        this.category = category;
        this.subcategory = subcategory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceCategory that = (ServiceCategory) o;
        return Objects.equals(category, that.category) && Objects.equals(subcategory, that.subcategory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, subcategory);
    }

    @Override
    public String toString() {
        return "ServiceCategories{" +
                "category='" + category + '\'' +
                ", subcategory='" + subcategory + '\'' +
                '}';
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }
}
