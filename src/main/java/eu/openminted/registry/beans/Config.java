package eu.openminted.registry.beans;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Created by stefanos on 14/6/2017.
 */

@Configuration
@EnableRedisHttpSession
@PropertySource(value = { "classpath:application.properties", "classpath:registry.properties"} )
public class Config {

    private static Logger logger = Logger.getLogger(Config.class);

    @Value("${redis.host}")
    private String host;

    @Value("${redis.port}")
    private String port;

    @Bean
    public LettuceConnectionFactory connectionFactory() {
        logger.info(String.format("Redis connection listens to %s:%s",host,port));
        return new LettuceConnectionFactory(host,Integer.parseInt(port));
    }
}