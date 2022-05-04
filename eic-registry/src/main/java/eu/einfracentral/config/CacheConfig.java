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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
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
    public static final String CACHE_CATALOGUES = "catalogues";
    public static final String CACHE_DATASOURCES = "datasources";

    protected RestTemplate restTemplate;

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
                new ConcurrentMapCache(CACHE_CATALOGUES),

                // NEEDED FOR registry-core
                new ConcurrentMapCache("resourceTypes"),
                new ConcurrentMapCache("resourceTypesIndexFields")
        ));
        return cacheManager;
    }

//    @Scheduled(initialDelay = 0, fixedRate = 120000) //run every 2 min
    @Scheduled(cron = "0 0 12 ? * *") // At 12:00:00pm every day
    public void updateCache() throws IOException, InterruptedException {
        // Update Cache URL
        URL updateCache = new URL("https://providers.eosc-portal.eu/stats-api/cache/updateCache");
        HttpURLConnection updateCon = (HttpURLConnection) updateCache.openConnection();
        updateCon.setRequestMethod("GET");
        // Promote Cache URL
        URL promoteCache = new URL("https://providers.eosc-portal.eu/stats-api/cache/promoteCache");
        HttpURLConnection promoteCon = (HttpURLConnection) promoteCache.openConnection();
        promoteCon.setRequestMethod("GET");
        int responseUpdateCode = updateCon.getResponseCode();
        logger.info(String.format("Updating Cache. Response Code: %d", responseUpdateCode));
        if (responseUpdateCode == HttpURLConnection.HTTP_OK) { // success
            logger.info("Success..Proceeding to Promoting Cache");
            TimeUnit.MINUTES.sleep(1);
            int responsePromoteCode = promoteCon.getResponseCode();
            logger.info(String.format("Promoting Cache. Response Code: %d", responsePromoteCode));
            if (responsePromoteCode == HttpURLConnection.HTTP_OK) { // success
                logger.info("Cache Updated and Promoted Successfully");
            } else {
                logger.info(String.format("An error occurred while trying to Promote Cache. Response Code: %d", responsePromoteCode));
            }
        } else {
            logger.info(String.format("An error occurred while trying to Update Cache. Response Code: %d", responseUpdateCode));
        }
    }
}
