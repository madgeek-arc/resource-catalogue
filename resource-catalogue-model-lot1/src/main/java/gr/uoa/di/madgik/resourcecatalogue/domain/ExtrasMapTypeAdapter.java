/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.domain;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

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
