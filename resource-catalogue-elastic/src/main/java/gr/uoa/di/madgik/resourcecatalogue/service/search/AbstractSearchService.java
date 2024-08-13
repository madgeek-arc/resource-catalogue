package gr.uoa.di.madgik.resourcecatalogue.service.search;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.elasticsearch.service.ElasticSearchService;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;

import java.util.*;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.*;

public abstract class AbstractSearchService extends ElasticSearchService implements SearchService {

    public AbstractSearchService(RestHighLevelClient client) {
        super(client);
    }

    /**
     * User can define custom logic for filtering resources based on the filters provided. The custom queries must be
     * applied on the existing {@param qBuilder} that is given.
     *
     * @param qBuilder   Accepts the {@link BoolQueryBuilder} object used to search.
     * @param allFilters Filters provided.
     * @return
     */
    public abstract BoolQueryBuilder customFilters(BoolQueryBuilder qBuilder, Map<String, List<Object>> allFilters);

    @Override
    public BoolQueryBuilder createQueryBuilder(FacetFilter filter) {
        BoolQueryBuilder qBuilder = new BoolQueryBuilder();

        // retrieve filters from FacetFilter object
        Map<String, List<Object>> allFilters = FacetFilterUtils.getFacetFilterFilters(filter);

        List<Object> searchFields = allFilters.remove(FacetFilterUtils.SEARCH_FIELDS);
        if (searchFields == null || searchFields.isEmpty()) {
            searchFields = Arrays.asList("resource_internal_id", "name", "title");
            // TODO: enable when searchable_area is configurable
            // searchFields = Collections.singletonList(FacetFilterUtils.SEARCHABLE_AREA);
        }

        if (filter.getKeyword() != null && !filter.getKeyword().equals("")) {
            String keyword = filter.getKeyword();
            List<String> phrases = new ArrayList<>();

            // find quoted terms and keep them as is
            List<String> parts = Arrays.stream(keyword.split("\"")).collect(Collectors.toList());
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
            if (keyword.split("[\\s-_,./;:'\\[\\]]").length == 1) {
                qBuilder.should(createMatchQuery(searchFields, Collections.singletonList(keyword), 1f, 0.5f));
            } else {
                for (char delimiter : " -_,./;:'[]".toCharArray()) {
                    if (keyword.contains("" + delimiter)) {
                        for (String word : keyword.split(String.format("\\%s", delimiter))) {
                            if (word.length() > 4) {
                                longKeywords.add(word);
                            } else {
                                shortKeywords.add(word);
                            }
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
            qBuilder.must(matchAllQuery());
        }

        // Custom Filters
        qBuilder = customFilters(qBuilder, allFilters);

        return qBuilder;
    }

    protected DisMaxQueryBuilder createDisMaxQuery(String key, List<Object> filters) {
        DisMaxQueryBuilder qb = disMaxQuery();
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
    protected DisMaxQueryBuilder createMatchQuery(List<Object> fields, List<String> keywords, Float boost, Float tieBreaker) {
        DisMaxQueryBuilder qb = disMaxQuery();
        for (Object field : fields) {
            for (String keyword : keywords) {
                /*for (int i = 0; i < keyword.length(); i++) {
                    String nextLetter = String.valueOf(keyword.charAt(i));
                    String possibleInput = keyword.replace(nextLetter, "[^\\s\\p{L}\\p{N}]");
                    qb.add(regexpQuery((String) field, ".*" + possibleInput + ".*"));
                }*/
                qb.add(matchQuery((String) field, keyword.toLowerCase()));
                qb.add(matchQuery((String) field, keyword.toUpperCase()));
                qb.add(regexpQuery((String) field, ".*" + keyword + ".*"));
                qb.add(regexpQuery((String) field, ".*" + keyword.toLowerCase() + ".*"));
                qb.add(regexpQuery((String) field, ".*" + keyword.toUpperCase() + ".*"));

                // Create Camel Case query
                keyword = keyword.substring(0, 1).toUpperCase() + keyword.substring(1).toLowerCase();
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
    protected DisMaxQueryBuilder createPhraseQuery(List<Object> fields, List<String> phrases, Float boost, Float tieBreaker) {
        DisMaxQueryBuilder qb = disMaxQuery();
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