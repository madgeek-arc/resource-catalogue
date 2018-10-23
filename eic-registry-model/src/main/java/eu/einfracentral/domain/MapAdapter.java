package eu.einfracentral.domain;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class MapAdapter<K, V> extends XmlAdapter<MapAdapter.GenericMap<K, V>, Map<K, V>> {

    @Override
    public Map<K, V> unmarshal(GenericMap<K, V> map) {
        HashMap<K, V> hashMap = new HashMap<>();
        for (GenericMapEntry<K, V> genericMapEntry : map.entries) {
            hashMap.put(genericMapEntry.key, genericMapEntry.value);
        }
        return hashMap;
    }

    @Override
    public GenericMap<K, V> marshal(Map<K, V> map) {
        GenericMap<K, V> genericMap = new GenericMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            GenericMapEntry<K, V> genericMapEntry =
                    new GenericMapEntry<>();
            genericMapEntry.key = entry.getKey();
            genericMapEntry.value = entry.getValue();
            genericMap.entries.add(genericMapEntry);
        }
        return genericMap;
    }

    public static class GenericMap<K, V> {
        public List<GenericMapEntry<K, V>> entries = new ArrayList<>();
    }

    public static class GenericMapEntry<K, V> {

        @XmlElement
        public K key;

        @XmlElement
        public V value;
    }
}