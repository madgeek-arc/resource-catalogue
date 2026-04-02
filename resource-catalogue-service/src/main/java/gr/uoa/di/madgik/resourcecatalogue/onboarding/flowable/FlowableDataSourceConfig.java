package gr.uoa.di.madgik.resourcecatalogue.onboarding.flowable;

import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class FlowableDataSourceConfig {

    @Bean
    @ConfigurationProperties("flowable.datasource")
    public DataSourceProperties flowableDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource flowableDataSource() {
        return flowableDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean
    public PlatformTransactionManager flowableTransactionManager(
            @Qualifier("flowableDataSource") DataSource flowableDataSource) {
        return new DataSourceTransactionManager(flowableDataSource);
    }

    @Bean
    public EngineConfigurationConfigurer<SpringAppEngineConfiguration> flowableEngineConfigurer(
            @Qualifier("flowableDataSource") DataSource flowableDataSource,
            @Qualifier("flowableTransactionManager") PlatformTransactionManager flowableTransactionManager) {
        return config -> {
            config.setDataSource(flowableDataSource);
            config.setTransactionManager(flowableTransactionManager);
        };
    }
}
