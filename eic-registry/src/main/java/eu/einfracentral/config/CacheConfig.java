package eu.einfracentral.config;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {
    private static final Logger logger = LogManager.getLogger(CacheConfig.class);

    public static final String CACHE_PROVIDERS = "providers";
    public static final String CACHE_VOCABULARIES = "vocabularies";
    public static final String CACHE_FEATURED = "featuredServices";
    public static final String CACHE_EVENTS = "events";
    public static final String CACHE_VISITS = "visits";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(Arrays.asList(

                new ConcurrentMapCache(CACHE_VISITS,
                        CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(500).build().asMap(), false),
                new ConcurrentMapCache(CACHE_FEATURED,
                        CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).maximumSize(50).build().asMap(), false),
                new ConcurrentMapCache(CACHE_PROVIDERS),
                new ConcurrentMapCache(CACHE_EVENTS),
                new ConcurrentMapCache(CACHE_VOCABULARIES),

                // NEEDED FOR registry-core
                new ConcurrentMapCache("resourceTypes"),
                new ConcurrentMapCache("resourceTypesIndexFields")
        ));
        return cacheManager;
    }

//    @Bean
//    public CacheManager visitsCacheManager() {
//        GuavaCacheManager cacheManager = new GuavaCacheManager();
//        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
//                .maximumSize(100)
//                .refreshAfterWrite(10, TimeUnit.MINUTES)
//                .expireAfterWrite(10, TimeUnit.MINUTES);
//        cacheManager.setCacheBuilder(cacheBuilder);
//
//        return cacheManager;
//    }

//    @Override
//    @Bean
//    public CacheManager cacheManager() {
//        return new ConcurrentMapCacheManager("resourceTypes", "resourceTypesIndexFields",
//                CACHE_EVENTS, CACHE_PROVIDERS, CACHE_VOCABULARIES, CACHE_FEATURED);
//    }

//    @Scheduled(cron = "0 0 12 1/1 * ?") // daily at 12:00 PM
//    @CacheEvict(value = "featuredServices", allEntries = true)
//    public void deleteFeaturedCache() {
//        logger.info(String.format("Deleting cache: '%s'", CACHE_FEATURED));
//    }
//
//    @Scheduled(cron = "0 0/10 * * * ?") // every ten minutes
//    @CacheEvict(value = "visits", allEntries = true)
//    public void deleteVisitsCache() {
//        logger.info(String.format("Deleting cache: '%s'", CACHE_VISITS));
//    }
}
