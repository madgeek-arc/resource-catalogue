package gr.uoa.di.madgik.resourcecatalogue.domain;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.HashMap;
import java.util.Map;

public class ExtrasMapTypeAdapter extends XmlAdapter<ExtrasMapType, Map<String, String>> {

    public Map<String, String> unmarshal(ExtrasMapType map) {
        HashMap<String, String> hashMap = new HashMap<>();

        for (ExtrasType extrasMapEntry : map.entry) {
            hashMap.put(extrasMapEntry.key, extrasMapEntry.value);
        }
        return hashMap;
    }

    @Override
    public ExtrasMapType marshal(Map<String, String> map) {
        if (map == null) {
            map = new HashMap<>();
        }
        ExtrasMapType extrasMap = new ExtrasMapType();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            ExtrasType extrasMapEntry = new ExtrasType();

            extrasMapEntry.key = entry.getKey();
            extrasMapEntry.value = entry.getValue();
            extrasMap.entry.add(extrasMapEntry);
        }
        return extrasMap;
    }
}
