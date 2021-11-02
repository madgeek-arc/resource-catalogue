package eu.einfracentral.config.security;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.data.redis.config.annotation.web.http.RedisHttpSessionConfiguration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;


@Configuration
@EnableRedisHttpSession
public class RedisConfig extends RedisHttpSessionConfiguration {

    private static final Logger logger = Logger.getLogger(RedisConfig.class);
    private final String redisHost;
    private final String redisPort;
    private final String redisPass;

    @Autowired
    public RedisConfig(@Value("${redis.host}") String host, @Value("${redis.port}") String port, @Value("${redis.password}") String password) {
        this.redisHost = host;
        this.redisPort = port;
        this.redisPass = password;
    }

    @Bean
    public JedisConnectionFactory connectionFactory() {
        logger.info(String.format("Redis connection listens to %s:%s", redisHost, redisPort));
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setHostName(redisHost);
        jedisConnectionFactory.setPort(Integer.parseInt(redisPort));
        jedisConnectionFactory.setUsePool(true);
        if (redisPass != null) jedisConnectionFactory.setPassword(redisPass);
        return jedisConnectionFactory;
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setCookiePath("/");
        return serializer;
    }
}