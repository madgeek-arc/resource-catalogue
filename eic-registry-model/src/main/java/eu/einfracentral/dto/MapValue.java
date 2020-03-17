package eu.einfracentral.dto;

import java.util.List;

public class MapValue {

    private String key;
    private List<Values> values;

    public MapValue(){
    }

    public MapValue(String key, List<Values> values){
        this.key = key;
        this.values = values;
    }

    public static class Values {

        private String name;
        private String url;

        public Values(){
        }

        public Values(String name, String url){
            this.name = name;
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<Values> getValues() {
        return values;
    }

    public void setValues(List<Values> values) {
        this.values = values;
    }
}
