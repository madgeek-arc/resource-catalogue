package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.registry.domain.Facet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

@Primary
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class ElasticFacetLabelService implements FacetLabelService {

    private static final Logger logger = LogManager.getLogger(ElasticFacetLabelService.class);
    private final RestHighLevelClient client;

    ElasticFacetLabelService(RestHighLevelClient client) {
        this.client = client;
    }

    @Override
    public List<Facet> generateLabels(List<Facet> facets) {
        Map<String, String> vocabularyValues = null;
        try {
            vocabularyValues = getIdNameFields();
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }

        for (Facet facet : facets) {
            Map<String, String> finalVocabularyValues = vocabularyValues;
            facet.getValues().forEach(value -> value.setLabel(getLabelElseKeepValue(value.getValue(), finalVocabularyValues)));
        }
        return facets;
    }

    private Map<String, String> getIdNameFields() throws IOException, ElasticsearchStatusException {
        Map<String, String> idNameMap = new TreeMap<>();

        final Scroll scroll = new Scroll(TimeValue.timeValueSeconds(1L));
        SearchRequest searchRequest = new SearchRequest("resourceTypes");
        searchRequest.scroll(scroll);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder
                .size(10000)
                .docValueField("resource_internal_id")
                .docValueField("name")
                .docValueField("title")
                .fetchSource(false)
                .explain(true);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        String scrollId = searchResponse.getScrollId();
        SearchHit[] searchHits = searchResponse.getHits().getHits();

        while (searchHits != null && searchHits.length > 0) {

            for (SearchHit hit : searchHits) {
                if (hit.getFields().containsKey("resource_internal_id")) {
                    String id = (String) hit.getFields().get("resource_internal_id").getValues().get(0);
                    if (hit.getFields().containsKey("name")) {
                        idNameMap.put(id, (String) hit.getFields().get("name").getValues().get(0));
                    } else if (hit.getFields().containsKey("title")) {
                        idNameMap.put(id, (String) hit.getFields().get("title").getValues().get(0));
                    }
                } else {
                    logger.error("Could not create id - name value. \nHit: {}", hit);
                }
            }

            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(scroll);
            searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
            scrollId = searchResponse.getScrollId();
            searchHits = searchResponse.getHits().getHits();
        }

        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(scrollId);
        ClearScrollResponse clearScrollResponse = client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
        boolean succeeded = clearScrollResponse.isSucceeded();
        if (!succeeded) {
            logger.error("clear scroll request failed...");
        }

        return idNameMap;
    }

    String toProperCase(String str, String delimiter, String newDelimiter) {
        if (str.equals("")) {
            str = "-";
        }
        StringJoiner joiner = new StringJoiner(newDelimiter);
        for (String s : str.split(delimiter)) {
            try {
                String s1;
                s1 = s.substring(0, 1).toUpperCase() + s.substring(1);
                joiner.add(s1);
            } catch (IndexOutOfBoundsException e) {
                return str;
            }
        }
        return joiner.toString();
    }

    String getLabelElseKeepValue(String value, Map<String, String> labels) {
        String ret = labels.get(value);
        if (ret == null) {
            ret = toProperCase(toProperCase(value, "-", "-"), "_", " ");
        }
        return ret;
    }
}
