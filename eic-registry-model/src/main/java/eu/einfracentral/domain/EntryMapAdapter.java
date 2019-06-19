package eu.einfracentral.domain;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// Important for the serialization and deserialization of EntryMap (created mainly for VocabularyEntry Class)
public final class EntryMapAdapter extends XmlAdapter<EntryMapAdapter.GenericMap, Map<String, VocabularyEntry>> {

    public Map<String, VocabularyEntry> unmarshal(GenericMap map) {
        HashMap<String, VocabularyEntry> hashMap = new HashMap<>();

        for (GenericMapEntry genericMapEntry : map.entries) {
            hashMap.put(genericMapEntry.key, genericMapEntry.value);
        }
        return hashMap;
    }

    @Override
    public GenericMap marshal(Map<String, VocabularyEntry> map) {
        GenericMap genericMap = new GenericMap();

        for (Map.Entry<String, VocabularyEntry> entry : map.entrySet()) {
            GenericMapEntry genericMapEntry = new GenericMapEntry();

            genericMapEntry.key = entry.getKey();
            genericMapEntry.value = entry.getValue();
            genericMap.entries.add(genericMapEntry);
        }
        return genericMap;
    }

    @XmlType(name = "genericMap", namespace = "http://einfracentral.eu")
    public static class GenericMap {
        @XmlElement(name="entry")
        public List<GenericMapEntry> entries = new ArrayList<>();
    }

    @XmlType(name = "genericMapEntry", namespace = "http://einfracentral.eu")
    public static class GenericMapEntry {

        @XmlElement
        public String key;

        @XmlElement
        public VocabularyEntry value;
    }
}