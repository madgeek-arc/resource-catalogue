package eu.einfracentral.service;

import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.SearchServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.regexpQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

@Service
@PropertySource({"classpath:application.properties", "classpath:registry.properties"})
public class SearchServiceEIC extends SearchServiceImpl implements SearchService {

    private static final Logger logger = LogManager.getLogger(SearchServiceEIC.class);

    public SearchServiceEIC() {
        super();
    }

    @Override
    public BoolQueryBuilder createQueryBuilder(FacetFilter filter) {
        BoolQueryBuilder qBuilder = new BoolQueryBuilder();

        // retrieve filters from FacetFilter object
        Map<String, List<Object>> allFilters = FacetFilterUtils.getFacetFilterFilters(filter);

        List<Object> searchFields = allFilters.remove(FacetFilterUtils.SEARCH_FIELDS);
        if (searchFields == null || searchFields.isEmpty()) {
            searchFields = Collections.singletonList(FacetFilterUtils.SEARCHABLE_AREA);
        }

        if (!filter.getKeyword().equals("")) {
            // create regexp disMaxQuery
            DisMaxQueryBuilder qb = QueryBuilders.disMaxQuery();
            for (Object field : searchFields) {
                qb.add(regexpQuery((String) field, ".*" + filter.getKeyword() + ".*"));
            }
            qb.boost(2f);
            qb.tieBreaker(0.7f);
            qBuilder.must(qb);
        } else {
            qBuilder.must(QueryBuilders.matchAllQuery());
        }

        for (Map.Entry<String, List<Object>> filters : allFilters.entrySet()) {
            if ("active".equals(filters.getKey())) {
                qBuilder.filter(createDisMaxQuery(filters.getKey(), filters.getValue()));
            } else if ("latest".equals(filters.getKey())) {
                qBuilder.filter(createDisMaxQuery(filters.getKey(), filters.getValue()));
            } else {
                qBuilder.must(createDisMaxQuery(filters.getKey(), filters.getValue()));
            }

        }
        return qBuilder;
    }

    private DisMaxQueryBuilder createDisMaxQuery(String key, List<Object> filters) {
        DisMaxQueryBuilder qb = QueryBuilders.disMaxQuery();
        for (Object f : filters) {
            qb.add(termQuery(key, (String) f));
        }
        qb.boost(2f);
        qb.tieBreaker(0.7f);
        return qb;
    }
}
