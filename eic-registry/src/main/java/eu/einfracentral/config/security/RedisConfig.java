package eu.einfracentral.config.security;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.redis.RedisOperationsSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.data.redis.config.annotation.web.http.RedisHttpSessionConfiguration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.HeaderHttpSessionStrategy;
import org.springframework.session.web.http.HttpSessionStrategy;


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

//    @Bean
//    public LettuceConnectionFactory connectionFactory() {
//        RedisStandaloneConfiguration redisConf = new RedisStandaloneConfiguration(redisHost, Integer.parseInt(redisPort));
//        if (redisPass != null) redisConf.setPassword(redisPass);
//        return new LettuceConnectionFactory(redisConf);
//    }

//    @Bean
//    public LettuceConnectionFactory connectionFactory() {
//        LettuceConnectionFactory redisConf = new LettuceConnectionFactory(redisHost, Integer.parseInt(redisPort));
//        if (redisPass != null) redisConf.setPassword(redisPass);
//        return redisConf;
//    }

//    @Bean
//    public HttpSessionStrategy httpSessionStrategy(){
//        HeaderHttpSessionStrategy headerHttpSessionStrategy = new HeaderHttpSessionStrategy();
//        headerHttpSessionStrategy.setHeaderName("EICSESSION");
//        return headerHttpSessionStrategy;
//    }


    @Bean
    public JedisConnectionFactory connectionFactory() {
        logger.info(String.format("Redis connection Factory created for %s:%s", redisHost, redisPort));
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setHostName(redisHost);
        jedisConnectionFactory.setPort(Integer.parseInt(redisPort));
        jedisConnectionFactory.setUsePool(true);
        if(redisPass != null) jedisConnectionFactory.setPassword(redisPass);
        return jedisConnectionFactory;
    }

//    @Bean
//    public CookieSerializer cookieSerializer() {
//        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
//        serializer.setCookieName("openAIRESession");
//        serializer.setCookiePath("/");
////        if(aai_mode.equalsIgnoreCase("production") || aai_mode.equalsIgnoreCase("beta"))
////            serializer.setDomainName(".openaire.eu");
////        serializer.setDomainName(".athenarc.gr");
//        logger.info("Serializer : " + serializer);
//        return serializer;
//    }
}