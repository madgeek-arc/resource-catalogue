package eu.einfracentral.recdb;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
public class DBConfiguration implements EnvironmentAware {

    public Environment environment;

    private static Logger logger = LogManager.getLogger(DBConfiguration.class);

    @Bean(name = "recdb.datasource")
    public DataSource dataSource(){
        return new HikariDataSource(hikariConfig());
    }

    private HikariConfig hikariConfig(){
//        logger.info("Connecting to Database @ "+environment.getRequiredProperty("recdb.datasource.url"));
        logger.info("Connecting to Database @ jdbc:postgresql://0.0.0.0:5432/recdb");
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("recdbRegistryCP");
        hikariConfig.setConnectionTestQuery("SELECT 1");
//        hikariConfig.setDriverClassName(environment.getRequiredProperty("recdb.datasource.driverClassName"));
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setMaximumPoolSize(15);
        hikariConfig.setIdleTimeout(120000);
        hikariConfig.setMinimumIdle(5);
//        hikariConfig.setJdbcUrl(environment.getRequiredProperty("recdb.datasource.url"));
        hikariConfig.setJdbcUrl("jdbc:postgresql://0.0.0.0:5432/recdb");
//        hikariConfig.setUsername(environment.getRequiredProperty("recdb.datasource.username"));
        hikariConfig.setUsername("postgres");
//        hikariConfig.setPassword(environment.getRequiredProperty("recdb.datasource.password"));
        hikariConfig.setPassword("admin");
        hikariConfig.addDataSourceProperty("cachePreStmts", "true"); // Enable Prepared Statement caching
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "25"); // How many PS cache, default: 25
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true"); // If supported use PS server-side
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true"); // Enable setAutoCommit
        hikariConfig.addDataSourceProperty("useLocalTransactionState", "true"); // Enable commit/rollbacks


        return hikariConfig;
    }


    @Override
    public void setEnvironment(Environment environment) {

    }
}
