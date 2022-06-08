package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.DataSourceBundle;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.registry.service.DataSourceService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import static eu.einfracentral.config.CacheConfig.*;

@org.springframework.stereotype.Service("dataSourceService")
public class DataSourceManager extends AbstractDataSourceManager implements DataSourceService<DataSourceBundle, DataSourceBundle> {

    @Autowired
    public DataSourceManager(@Qualifier("providerManager") ResourceManager<ProviderBundle> resourceManager) {
        super(DataSourceBundle.class);
    }

    @Override
    public String getResourceType() {
        return "dataSource";
    }

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED, CACHE_DATASOURCES}, allEntries = true)
    public DataSourceBundle addDataSource(DataSourceBundle dataSourceBundle, Authentication auth) {
        return null;
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    @CacheEvict(cacheNames = {CACHE_PROVIDERS, CACHE_FEATURED, CACHE_DATASOURCES}, allEntries = true)
    public DataSourceBundle updateDataSource(DataSourceBundle dataSourceBundle, String comment, Authentication auth) {
        return null;
    }
}
