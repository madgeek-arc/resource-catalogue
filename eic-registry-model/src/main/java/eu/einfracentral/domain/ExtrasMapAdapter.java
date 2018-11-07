package eu.einfracentral.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class ExtrasMapAdapter extends XmlAdapter<ExtrasMapAdapter.ExtrasMap, Map<String, String>> {

    public Map<String, String> unmarshal(ExtrasMap map) {
        HashMap<String, String> hashMap = new HashMap<>();

        for (Extras genericMapEntry : map.entries) {
            hashMap.put(genericMapEntry.key, genericMapEntry.value);
        }
        return hashMap;
    }

    @Override
    public ExtrasMap marshal(Map<String, String> map) {
        ExtrasMap genericMap = new ExtrasMap();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            Extras genericMapEntry = new Extras();

            genericMapEntry.key = entry.getKey();
            genericMapEntry.value = entry.getValue();
            genericMap.entries.add(genericMapEntry);
        }
        return genericMap;
    }

    public static class ExtrasMap {
        @XmlElement(name="entry")
        public List<Extras> entries = new ArrayList<>();
    }

    public static class Extras {

        @XmlElement
        public String key;

        @XmlElement
        public String value;
    }
}