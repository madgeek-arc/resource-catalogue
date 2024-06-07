package gr.uoa.di.madgik.resourcecatalogue.config;

public class Properties {

    private Properties() {
    }

    public static class Cache {
        private Cache() {
        }

        public static final String CACHE_PROVIDERS = "providers";
        public static final String CACHE_VOCABULARIES = "vocabularies";
        public static final String CACHE_VOCABULARY_MAP = "vocabulary_map";
        public static final String CACHE_VOCABULARY_TREE = "vocabulary_tree";
        public static final String CACHE_FEATURED = "featuredServices";
        public static final String CACHE_EVENTS = "events";
        public static final String CACHE_SERVICE_EVENTS = "service_events";
        public static final String CACHE_VISITS = "visits";
    }
}
