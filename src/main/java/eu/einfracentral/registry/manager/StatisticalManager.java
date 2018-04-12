package eu.einfracentral.registry.manager;

import eu.einfracentral.registry.service.StatisticalService;
import eu.openminted.registry.core.configuration.ElasticConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.Term;
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
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.valuecount.ValueCountAggregator;
import org.elasticsearch.search.aggregations.pipeline.PipelineAggregatorBuilders;
import org.elasticsearch.search.aggregations.pipeline.SimpleValue;
import org.elasticsearch.search.aggregations.pipeline.cumulativesum.CumulativeSumPipelineAggregationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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


        QueryBuilder queryBuilder = buildQuery(serviceId,"rating");


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

    private QueryBuilder buildQuery(String serviceId, String typeOf){

        //TERMS
        //last month
        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.roll(Calendar.MONTH,false);
        QueryBuilder queryBuilder = null;

        if(typeOf.equals("rating")){
            queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("service_id",serviceId))
                    .filter(QueryBuilders.rangeQuery("instant").from(c.getTime().getTime()).to(new Date().getTime()))
                    .filter(QueryBuilders.termsQuery("type","RATING"));

        }else if(typeOf.equals("favourites")){
            queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termsQuery("service_id",serviceId))
                    .filter(QueryBuilders.rangeQuery("instant").from(c.getTime().getTime()).to(new Date().getTime()))
                    .filter(QueryBuilders.termsQuery("type","FAVOURITE"));
        }

        return queryBuilder;
    }

    @Override
    public Map<String, Integer> averageFavouritesByService(String serviceId) {
        Map<String, Integer> results = new HashMap<>();

        Client client = elastic.client();

        //AGGREGATIONS
        final DateHistogramAggregationBuilder dateHistogramAggregationBuilder= AggregationBuilders.
                dateHistogram("months").
                field("instant").dateHistogramInterval(DateHistogramInterval.DAY).format("yyyy-MM-dd")
                .subAggregation(AggregationBuilders.terms("score").field("score"));


        QueryBuilder queryBuilder = buildQuery(serviceId,"favourites");


        SearchRequestBuilder searchBuilder=client.prepareSearch("event").setTypes("general").setQuery(queryBuilder)
                .addAggregation(dateHistogramAggregationBuilder);

        SearchResponse response=searchBuilder.execute().actionGet();


        InternalDateHistogram terms = response.getAggregations().get("months");
        List<Histogram.Bucket> buckets = terms.getBuckets();
        try {
            int totalDocCountTrue=0;
            int totalDocCountFalse=0;
            for (Histogram.Bucket bucket : buckets) {
                Terms subTerm = bucket.getAggregations().get("score");
                if(subTerm.getBuckets()!=null) {
                    for (Terms.Bucket subBucket : subTerm.getBuckets()) {
                        if(subBucket.getKeyAsNumber().intValue()==0)
                            totalDocCountFalse+= subBucket.getDocCount();
                        else if (subBucket.getKeyAsNumber().intValue()==1)
                            totalDocCountTrue+= subBucket.getDocCount();
                    }
                }
                if((totalDocCountTrue-totalDocCountFalse)<=0)
                    results.put(bucket.getKeyAsString(),0);
                else
                    results.put(bucket.getKeyAsString(),totalDocCountTrue-totalDocCountFalse);
            }

            return results;
        }catch(Exception e){
            logger.error("Parsing aggregations ", e);
        }

        return results;
    }
}
