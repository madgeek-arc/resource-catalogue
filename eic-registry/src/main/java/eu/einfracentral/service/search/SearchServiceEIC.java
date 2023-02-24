package eu.einfracentral.service.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@PropertySource({"classpath:application.properties", "classpath:registry.properties"})
public class SearchServiceEIC extends AbstractSearchService {

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
