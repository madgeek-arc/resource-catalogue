package gr.uoa.di.madgik.resourcecatalogue.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import gr.uoa.di.madgik.resourcecatalogue.annotation.VocabularyValidation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;
import java.util.Objects;

public class PersistentIdentitySystem {

    /**
     * Specify the EntityType to which the persistent identifier is referring to.
     */
    @XmlElement(required = true)
    @Schema
    @FieldValidation()
    @VocabularyValidation(type = Vocabulary.Type.DS_RESEARCH_ENTITY_TYPE)
    private String persistentIdentityEntityType;

    /**
     * Specify the list of persistent identifier schemes used to refer to EntityTypes.
     */
    @XmlElementWrapper(required = true, name = "persistentIdentityEntityTypeSchemes")
    @XmlElement(name = "persistentIdentityEntityType")
    @Schema
    @FieldValidation()
    @VocabularyValidation(type = Vocabulary.Type.DS_PERSISTENT_IDENTITY_SCHEME)
    private List<String> persistentIdentityEntityTypeSchemes;

    public PersistentIdentitySystem() {
    }

    public PersistentIdentitySystem(String persistentIdentityEntityType, List<String> persistentIdentityEntityTypeSchemes) {
        this.persistentIdentityEntityType = persistentIdentityEntityType;
        this.persistentIdentityEntityTypeSchemes = persistentIdentityEntityTypeSchemes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersistentIdentitySystem that = (PersistentIdentitySystem) o;
        return Objects.equals(persistentIdentityEntityType, that.persistentIdentityEntityType) && Objects.equals(persistentIdentityEntityTypeSchemes, that.persistentIdentityEntityTypeSchemes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(persistentIdentityEntityType, persistentIdentityEntityTypeSchemes);
    }

    @Override
    public String toString() {
        return "PersistentIdentitySystem{" +
                "persistentIdentityEntityType='" + persistentIdentityEntityType + '\'' +
                ", persistentIdentityEntityTypeSchemes=" + persistentIdentityEntityTypeSchemes +
                '}';
    }

    public String getPersistentIdentityEntityType() {
        return persistentIdentityEntityType;
    }

    public void setPersistentIdentityEntityType(String persistentIdentityEntityType) {
        this.persistentIdentityEntityType = persistentIdentityEntityType;
    }

    public List<String> getPersistentIdentityEntityTypeSchemes() {
        return persistentIdentityEntityTypeSchemes;
    }

    public void setPersistentIdentityEntityTypeSchemes(List<String> persistentIdentityEntityTypeSchemes) {
        this.persistentIdentityEntityTypeSchemes = persistentIdentityEntityTypeSchemes;
    }
}
