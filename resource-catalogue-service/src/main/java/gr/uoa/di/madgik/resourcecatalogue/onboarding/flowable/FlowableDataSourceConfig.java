package gr.uoa.di.madgik.resourcecatalogue.onboarding.flowable;

import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;

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
    public SpringProcessEngineConfiguration processEngineConfiguration(
            @Qualifier("flowableDataSource") DataSource flowableDataSource,
            PlatformTransactionManager transactionManager) throws IOException {

        SpringProcessEngineConfiguration config = new SpringProcessEngineConfiguration();
        config.setDataSource(flowableDataSource);
        config.setTransactionManager(transactionManager);
        config.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        config.setDeploymentResources(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:processes/*.bpmn20.xml"));
        return config;
    }

    @Bean
    public ProcessEngineFactoryBean processEngine(SpringProcessEngineConfiguration processEngineConfiguration) {
        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
        factoryBean.setProcessEngineConfiguration(processEngineConfiguration);
        return factoryBean;
    }
}
