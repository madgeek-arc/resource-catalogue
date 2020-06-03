package eu.einfracentral.service;

import eu.einfracentral.utils.FacetFilterUtils;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.SearchServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.Strings;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.*;

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

        if (filter.getKeyword() != null && !filter.getKeyword().equals("")) {
            String keyword = filter.getKeyword();
            List<String> phrases = new ArrayList<>();

            // find quoted terms and keep them as is
            List<String> parts = Arrays.stream(Strings.split(keyword, '"')).collect(Collectors.toList());
            if (parts.size() > 1 && parts.size() % 2 == 1) {
                for (int i = 0; i < parts.size(); i++) {
                    if (i % 2 == 1) {
                        phrases.add(parts.get(i));
                    }
                }
            }
            keyword = keyword.replace("\"", "");
            phrases.add(keyword);

            // create phrase query to match search phrase and quoted phrases
            qBuilder.should(createPhraseQuery(searchFields, phrases, 4f, 1f));

            // split search phrase to keywords using delimiters
            List<String> longKeywords = new ArrayList<>();
            List<String> shortKeywords = new ArrayList<>();
            for (char delimiter : " -_,./;:'[]".toCharArray()) {
                if (keyword.contains("" + delimiter)) {
                    for (String word : keyword.split(String.format("\\%s", delimiter))) {
                        if (word.length() > 3) {
                            longKeywords.add(word);
                        } else {
                            shortKeywords.add(word);
                        }
                    }
                }
            }

            // create fuzzy query for long keywords
            if (!longKeywords.isEmpty()) {
                qBuilder.should(createMatchQuery(searchFields, longKeywords, 1f, 0.2f));
            }

            // create fuzzy query for short keywords
            if (!shortKeywords.isEmpty()) {
                qBuilder.should(createMatchQuery(searchFields, shortKeywords, 0.2f, 0.1f));
            }

            qBuilder.minimumShouldMatch(1);
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

    /**
     * Creates a query for the keywords in all given search fields.
     *
     * @param fields     The search fields.
     * @param keywords   The search keywords.
     * @param boost      A multiplier for the score of the query (parameter of the {@link DisMaxQueryBuilder}).
     * @param tieBreaker (parameter of the {@link DisMaxQueryBuilder})
     * @return {@link DisMaxQueryBuilder}
     */
    private DisMaxQueryBuilder createMatchQuery(List<Object> fields, List<String> keywords, Float boost, Float tieBreaker) {
        DisMaxQueryBuilder qb = QueryBuilders.disMaxQuery();
        for (Object field : fields) {
            for (String keyword : keywords) {
                /*for (int i = 0; i < keyword.length(); i++) {
                    String nextLetter = String.valueOf(keyword.charAt(i));
                    String possibleInput = keyword.replace(nextLetter, "[^\\s\\p{L}\\p{N}]");
                    qb.add(regexpQuery((String) field, ".*" + possibleInput + ".*"));
                }*/
                qb.add(matchQuery((String) field, keyword));
                qb.add(regexpQuery((String) field, ".*" + keyword + ".*"));
            }
        }
        qb.boost(boost);
        qb.tieBreaker(tieBreaker);
        return qb;
    }

    /**
     * Creates a phrase query for all the given phrases in all the given search fields.
     *
     * @param fields     The search fields.
     * @param phrases    The search phrases.
     * @param boost      A multiplier for the score of the query (parameter of the {@link DisMaxQueryBuilder}).
     * @param tieBreaker (parameter of the {@link DisMaxQueryBuilder})
     * @return {@link DisMaxQueryBuilder}
     */
    private DisMaxQueryBuilder createPhraseQuery(List<Object> fields, List<String> phrases, Float boost, Float tieBreaker) {
        DisMaxQueryBuilder qb = QueryBuilders.disMaxQuery();
        for (Object field : fields) {
            for (String phrase : phrases) {
                qb.add(matchPhraseQuery((String) field, phrase));
            }
        }
        qb.boost(boost);
        qb.tieBreaker(tieBreaker);
        return qb;
    }
}
