package eu.einfracentral.config;

import com.google.common.cache.CacheBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {
    private static final Logger logger = LogManager.getLogger(CacheConfig.class);

    public static final String CACHE_PROVIDERS = "providers";
    public static final String CACHE_VOCABULARIES = "vocabularies";
    public static final String CACHE_VOCABULARY_MAP = "vocabulary_map";
    public static final String CACHE_VOCABULARY_TREE = "vocabulary_tree";
    public static final String CACHE_FEATURED = "featuredServices";
    public static final String CACHE_EVENTS = "events";
    public static final String CACHE_SERVICE_EVENTS = "service_events";
    public static final String CACHE_VISITS = "visits";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(Arrays.asList(

                new ConcurrentMapCache(CACHE_VISITS,
                        CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(2000).build().asMap(), false),
                new ConcurrentMapCache(CACHE_FEATURED,
                        CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).maximumSize(50).build().asMap(), false),
                new ConcurrentMapCache(CACHE_PROVIDERS),
                new ConcurrentMapCache(CACHE_EVENTS),
                new ConcurrentMapCache(CACHE_SERVICE_EVENTS),
                new ConcurrentMapCache(CACHE_VOCABULARIES),
                new ConcurrentMapCache(CACHE_VOCABULARY_MAP),
                new ConcurrentMapCache(CACHE_VOCABULARY_TREE),

                // NEEDED FOR registry-core
                new ConcurrentMapCache("resourceTypes"),
                new ConcurrentMapCache("resourceTypesIndexFields")
        ));
        return cacheManager;
    }
}
