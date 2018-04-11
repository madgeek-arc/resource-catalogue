package eu.einfracentral.registry.manager;

import eu.einfracentral.registry.service.StatisticalService;
import eu.openminted.registry.core.configuration.ElasticConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.histogram.InternalDateHistogram;
import org.elasticsearch.search.aggregations.pipeline.PipelineAggregatorBuilders;
import org.elasticsearch.search.aggregations.pipeline.SimpleValue;
import org.elasticsearch.search.aggregations.pipeline.cumulativesum.CumulativeSumPipelineAggregationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service("statisticalService")
public class StatisticalManager implements StatisticalService {

    private static Logger logger = LogManager.getLogger(StatisticalManager.class);

    @Autowired
    private ElasticConfiguration elastic;

    @Value("${elastic.aggregation.topHitsSize : 100}")
    private int topHitsSize;

    @Value("${elastic.aggregation.bucketSize : 100}")
    private int bucketSize;

    @Override
    public Map<String, Float> averageRatingByService(String serviceId) {

        Map<String, Float> results = new HashMap<>();

        Client client = elastic.client();

        //AGGREGATIONS
        CumulativeSumPipelineAggregationBuilder cumSum = PipelineAggregatorBuilders.cumulativeSum("cum_sum", "rating");
        final DateHistogramAggregationBuilder dateHistogramAggregationBuilder= AggregationBuilders.
                        dateHistogram("months").
                        field("instant").dateHistogramInterval(DateHistogramInterval.DAY).format("yyyy-MM-dd")
                .subAggregation(AggregationBuilders.
                        sum("rating").
                        field("score"))
                .subAggregation(cumSum);


        //TERMS

        //last month
        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.roll(Calendar.MONTH,false);

        QueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("service_id",serviceId))
                .filter(QueryBuilders.rangeQuery("instant").from(c.getTime().getTime()).to(new Date().getTime()));

        SearchRequestBuilder searchBuilder=client.prepareSearch("event").setTypes("general").setQuery(queryBuilder)
                .addAggregation(dateHistogramAggregationBuilder);

        SearchResponse response=searchBuilder.execute().actionGet();


        InternalDateHistogram terms = response.getAggregations().get("months");
        List<Histogram.Bucket> buckets = terms.getBuckets();
        try {
            int totalDocCount=0;
            for (Histogram.Bucket bucket : buckets) {
                totalDocCount += Math.toIntExact(bucket.getDocCount());
                SimpleValue cumSumVal = bucket.getAggregations().get("cum_sum");
                results.put(bucket.getKeyAsString(), Float.parseFloat(cumSumVal.getValueAsString())/totalDocCount);
            }
            return results;
        }catch(Exception e){
            logger.error("Parsing aggregations ", e);
        }

        return results;
    }
}
