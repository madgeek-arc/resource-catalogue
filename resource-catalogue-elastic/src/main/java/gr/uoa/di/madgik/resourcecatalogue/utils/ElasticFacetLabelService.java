package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.registry.domain.Facet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Primary
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class ElasticFacetLabelService implements FacetLabelService {

    private static final Logger logger = LogManager.getLogger(ElasticFacetLabelService.class);
    private final RestHighLevelClient client;

    @org.springframework.beans.factory.annotation.Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    ElasticFacetLabelService(RestHighLevelClient client) {
        this.client = client;
    }

    @Override
    public List<Facet> generateLabels(List<Facet> facets) {
        Map<String, String> vocabularyValues = getIdNameFields();

        for (Facet facet : facets) {
            facet.getValues().forEach(value -> value.setLabel(getLabelElseKeepValue(value.getValue(), vocabularyValues)));
        }
        return facets;
    }

    private Map<String, String> getIdNameFields() {
        Map<String, String> idNameMap = new TreeMap<>();
        SearchRequest searchRequest = new SearchRequest();

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder
                .from(0)
                .size(10000)
                .docValueField("*_id")
                .docValueField("resource_internal_id")
                .docValueField("name")
                .fetchSource(false)
                .explain(true);
        searchRequest.source(searchSourceBuilder);

        SearchResponse response = null;
        try {
            response = client.search(searchRequest, RequestOptions.DEFAULT);

            List<SearchHit> hits = Arrays.stream(response.getHits().getHits()).collect(Collectors.toList());

            for (SearchHit hit : hits) {
                hit.getFields().remove("_id");
                if (hit.getFields().containsKey("resource_internal_id") && hit.getFields().containsKey("name")) {
                    idNameMap.put((String) hit.getFields().get("resource_internal_id").getValues().get(0), (String) hit.getFields().get("name").getValues().get(0));
                } else if (hit.getFields().containsKey("name") && hit.getFields().size() > 1) {
                    String name = (String) hit.getFields().remove("name").getValues().get(0);
                    List<DocumentField> id = (List<DocumentField>) hit.getFields().values();
                    idNameMap.put((String) id.get(0).getValues().get(0), name);
                }
            }
        } catch (IOException e) {
            logger.error("Error retrieving Id / Name values from all resources.", e);
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
