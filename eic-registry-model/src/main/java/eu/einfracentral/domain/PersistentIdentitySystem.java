package eu.einfracentral.domain;

import eu.einfracentral.annotation.FieldValidation;
import eu.einfracentral.annotation.VocabularyValidation;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;

public class PersistentIdentitySystem {

    /**
     * Specify the EntityType to which the persistent identifier is referring to.
     */
    @XmlElement(required = true)
    @ApiModelProperty(position = 1, required = true)
    @FieldValidation()
    @VocabularyValidation(type = Vocabulary.Type.DS_RESEARCH_ENTITY_TYPE)
    private String persistentIdentityEntityType;

    /**
     * Specify the list of persistent identifier schemes used to refer to EntityTypes.
     */
    @XmlElementWrapper(required = true, name = "persistentIdentityEntityTypeSchemes")
    @XmlElement(name = "persistentIdentityEntityType")
    @ApiModelProperty(position = 2, required = true)
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
