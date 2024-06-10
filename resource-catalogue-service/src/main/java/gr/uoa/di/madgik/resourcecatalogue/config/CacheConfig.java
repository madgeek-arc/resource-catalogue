package gr.uoa.di.madgik.resourcecatalogue.config;

import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static gr.uoa.di.madgik.resourcecatalogue.config.Properties.Cache.*;

@Configuration
@EnableCaching
public class CacheConfig {
    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    @Primary
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(Arrays.asList(

                new ConcurrentMapCache(CACHE_VISITS,
                        CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(2000).build().asMap(), false),
                new ConcurrentMapCache(CACHE_FEATURED,
                        CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).maximumSize(5).build().asMap(), false),
                new ConcurrentMapCache(CACHE_PROVIDERS,
                        CacheBuilder.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).maximumSize(10).build().asMap(), false),
                new ConcurrentMapCache(CACHE_EVENTS,
                        CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).maximumSize(1000).build().asMap(), false),
                new ConcurrentMapCache(CACHE_SERVICE_EVENTS,
                        CacheBuilder.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).maximumSize(1000).build().asMap(), false),
                new ConcurrentMapCache(CACHE_VOCABULARIES,
                        CacheBuilder.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).maximumSize(50).build().asMap(), false),
                new ConcurrentMapCache(CACHE_VOCABULARY_MAP,
                        CacheBuilder.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).maximumSize(50).build().asMap(), false),
                new ConcurrentMapCache(CACHE_VOCABULARY_TREE,
                        CacheBuilder.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).maximumSize(50).build().asMap(), false),

                // NEEDED FOR registry-core
                new ConcurrentMapCache("resourceTypes"),
                new ConcurrentMapCache("resourceTypesIndexFields")
        ));
        return cacheManager;
    }
}
