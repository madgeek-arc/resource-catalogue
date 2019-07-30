package eu.einfracentral.dto;

import eu.einfracentral.domain.Vocabulary;

public class VocabularyTuple {
    Vocabulary key;
    Vocabulary value;

    public VocabularyTuple() {
    }

    public VocabularyTuple(Vocabulary key, Vocabulary value) {
        this.key = key;
        this.value = value;
    }

    public Vocabulary getKey() {
        return key;
    }

    public void setKey(Vocabulary key) {
        this.key = key;
    }

    public Vocabulary getValue() {
        return value;
    }

    public void setValue(Vocabulary value) {
        this.value = value;
    }
}
