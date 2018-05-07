package eu.einfracentral.manager;

import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.service.*;
import eu.openminted.registry.core.configuration.ElasticConfiguration;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.*;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.*;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.histogram.*;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.pipeline.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("statisticsService")
public class StatisticsManager implements StatisticsService {
    private static Logger logger = LogManager.getLogger(StatisticsManager.class);
    @Autowired
    private ElasticConfiguration elastic;
    @Autowired
    private AnalyticsService analyticsService;
    @Autowired
    private ProviderService providerService;

    @Override
    public Map<String, Float> ratings(String id) {
        List<Histogram.Bucket> buckets = ((InternalDateHistogram) (elastic
                .client()
                .prepareSearch("event")
                .setTypes("general")
                .setQuery(getEventQueryBuilder(id, "RATING"))
                .addAggregation(AggregationBuilders.dateHistogram("months")
                                                   .field("instant")
                                                   .dateHistogramInterval(DateHistogramInterval.DAY)
                                                   .format("yyyy-MM-dd")
                                                   .subAggregation(AggregationBuilders.sum("rating").field("value"))
                                                   .subAggregation(PipelineAggregatorBuilders.cumulativeSum("cum_sum", "rating"))
                ).execute()
                .actionGet()
                .getAggregations()
                .get("months")))
                .getBuckets();
        long totalDocCount = buckets.stream().mapToLong(MultiBucketsAggregation.Bucket::getDocCount).sum();
        return buckets.stream().collect(Collectors.toMap(
                MultiBucketsAggregation.Bucket::getKeyAsString,
                e -> Float.parseFloat(((SimpleValue) e.getAggregations().get("cum_sum")).getValueAsString()) / totalDocCount
        ));
    }

    private InternalDateHistogram histogram(String id, String eventType) {
        return elastic
                .client()
                .prepareSearch("event")
                .setTypes("general")
                .setQuery(getEventQueryBuilder(id, eventType))
                .addAggregation(AggregationBuilders
                                        .dateHistogram("months")
                                        .field("instant")
                                        .dateHistogramInterval(DateHistogramInterval.DAY)
                                        .format("yyyy-MM-dd")
                                        .subAggregation(AggregationBuilders.terms("value").field("value"))
                ).execute()
                .actionGet()
                .getAggregations()
                .get("months");
    }

    private QueryBuilder getEventQueryBuilder(String serviceId, String eventType) {
        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.roll(Calendar.MONTH, false);
        return QueryBuilders.boolQuery()
                            .filter(QueryBuilders.termsQuery("service", serviceId))
                            .filter(QueryBuilders.rangeQuery("instant").from(c.getTime().getTime()).to(new Date().getTime()))
                            .filter(QueryBuilders.termsQuery("type", eventType));
    }

    @Override
    public Map<String, Integer> externals(String id) {
        return counts(id, "EXTERNAL");
    }

    private Map<String, Integer> counts(String id, String eventType) {
        return histogram(id, eventType, null).getBuckets().stream().collect(
                Collectors.toMap(e -> e.getKeyAsString(), e -> (int) e.getDocCount())
        );
    }

    @Override
    public Map<String, Integer> internals(String id) {
        return counts(id, "INTERNAL");
    }

    @Override
    public Map<String, Integer> pFavourites(String id) {
        return providerService.getServices(id)
                              .stream()
                              .flatMap(s -> favourites(s.getId()).entrySet().stream())
                              .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));
    }

    @Override
    public Map<String, Integer> visits(String id) {
        return analyticsService.getVisitsForLabel("service/" + id);
    }

    @Override
    public Map<String, Float> pRatings(String id) {
        Map<String, Float> ret = new HashMap<>();
        providerService.getServices(id).stream().forEach(s -> {
            Map<String, Float> ratings = ratings(s.getId());
            ratings.forEach((k, v) -> {
                ret.putIfAbsent(k, 0f);
                ret.put(k, ret.get(k) + v);
            });
        });
        return ret;
    }

    @Override
    public Map<String, Integer> pExternals(String id) {
        Map<String, Integer> ret = new HashMap<>();
        providerService.getServices(id).stream().forEach(s -> {
            Map<String, Integer> externals = externals(s.getId());
            externals.forEach((k, v) -> {
                ret.putIfAbsent(k, 0);
                ret.put(k, ret.get(k) + v);
            });
        });
        return ret;
    }

    @Override
    public Map<String, Integer> pInternals(String id) {
        return providerService.getServices(id)
                              .stream()
                              .flatMap(s -> internals(s.getId()).entrySet().stream())
                              .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));
    }

    @Override
    public Map<String, Integer> favourites(String id) {
        Map<String, Integer> ret = new HashMap<>();
        final long[] totalDocCounts = new long[2]; //0 - false documents, ie unfavourites, 1 - true documents, ie favourites
        try {
            ret = histogram(id, "FAVOURITE", null).getBuckets().stream().collect(
                    Collectors.toMap(
                            MultiBucketsAggregation.Bucket::getKeyAsString,
                            bucket -> {
                                Terms subTerm = bucket.getAggregations().get("score");
                                if (subTerm.getBuckets() != null) {
                                    totalDocCounts[0] += subTerm.getBuckets().stream().mapToLong(
                                            subBucket -> subBucket.getKeyAsNumber().intValue() == 0 ? subBucket.getDocCount() : 0
                                    ).sum();
                                    totalDocCounts[1] += subTerm.getBuckets().stream().mapToLong(
                                            subBucket -> subBucket.getKeyAsNumber().intValue() == 1 ? subBucket.getDocCount() : 0
                                    ).sum();
                                }
                                return (int) Math.max(totalDocCounts[1] - totalDocCounts[0], 0);
                            }
                    ));
        } catch (Exception e) {
            logger.error("Parsing aggregations ", e);
        }
        return ret;
    }

    @Override
    public Map<String, Integer> pVisits(String id) {
        return providerService.getServices(id)
                              .stream()
                              .flatMap(s -> visits(s.getId()).entrySet().stream())
                              .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));
    }

    @Override
    public Map<String, Float> pVisitation(String id) {
        Map<String, Integer> counts = providerService.getServices(id).stream().collect(Collectors.toMap(
                Service::getName,
                s -> visits(s.getId()).values().stream().mapToInt(Integer::intValue).sum()
        ));
        int grandTotal = counts.values().stream().mapToInt(Integer::intValue).sum();
        return counts.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> ((float) v.getValue()) / grandTotal));
    }
}
