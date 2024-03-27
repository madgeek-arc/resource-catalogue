package gr.uoa.di.madgik.resourcecatalogue.service.search;

import gr.uoa.di.madgik.registry.service.DefaultSearchService;
import gr.uoa.di.madgik.registry.service.SearchService;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service("eicSearchService")
public class CustomSearchService extends DefaultSearchService implements SearchService {

    public CustomSearchService(DataSource dataSource) {
        super(dataSource);
    }
}
