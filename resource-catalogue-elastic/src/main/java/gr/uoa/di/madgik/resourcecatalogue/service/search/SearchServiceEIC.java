package gr.uoa.di.madgik.resourcecatalogue.service.search;

import gr.uoa.di.madgik.registry.service.SearchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;

import java.util.List;
import java.util.Map;

//@Service
public class SearchServiceEIC extends AbstractSearchService implements SearchService {

    private static final Logger logger = LogManager.getLogger(SearchServiceEIC.class);

    public SearchServiceEIC(RestHighLevelClient elasticsearchClient) {
        super(elasticsearchClient);
    }

    @Override
    public BoolQueryBuilder customFilters(BoolQueryBuilder qBuilder, Map<String, List<Object>> allFilters) {
        for (Map.Entry<String, List<Object>> filters : allFilters.entrySet()) {

            switch (filters.getKey()) {
                case "active":
                    qBuilder.filter(createDisMaxQuery(filters.getKey(), filters.getValue()));
                    break;

                default:
                    qBuilder.must(createDisMaxQuery(filters.getKey(), filters.getValue()));
                    break;
            }
        }
        return qBuilder;
    }
}
