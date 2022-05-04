package eu.einfracentral.recdb;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DBConfiguration {

    private static final Logger logger = LogManager.getLogger(DBConfiguration.class);

    @Bean(name = "recdbDataSource")
    public DataSource recdbDataSource() {
        return new HikariDataSource(hikariConfig());
    }

    @Value("${recdb.datasource.url}")
    public String recdbUrl;

    @Value("${recdb.datasource.driverClassName}")
    public String driverClassName;

    @Value("${recdb.datasource.username}")
    public String recdbName;

    @Value("${recdb.datasource.password}")
    public String recdbPass;

    private HikariConfig hikariConfig() {
        logger.info("Connecting to Database @ {}", recdbUrl);
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("recdbRegistryCP");
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setDriverClassName(driverClassName);
        hikariConfig.setMaximumPoolSize(15);
        hikariConfig.setIdleTimeout(120000);
        hikariConfig.setMinimumIdle(5);
        hikariConfig.setJdbcUrl(recdbUrl);
        hikariConfig.setUsername(recdbName);
        hikariConfig.setPassword(recdbPass);
        hikariConfig.addDataSourceProperty("cachePreStmts", "true"); // Enable Prepared Statement caching
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "25"); // How many PS cache, default: 25
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true"); // If supported use PS server-side
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true"); // Enable setAutoCommit
        hikariConfig.addDataSourceProperty("useLocalTransactionState", "true"); // Enable commit/rollbacks


        return hikariConfig;
    }
}
