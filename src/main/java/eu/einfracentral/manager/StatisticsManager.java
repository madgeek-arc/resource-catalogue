package eu.einfracentral.manager;

import eu.einfracentral.registry.service.*;
import eu.einfracentral.service.*;
import eu.openminted.registry.core.configuration.ElasticConfiguration;
import eu.openminted.registry.core.domain.FacetFilter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.*;
import org.apache.logging.log4j.*;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.*;
import org.elasticsearch.search.aggregations.bucket.histogram.*;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.pipeline.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 23/04/18.
 */
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
        Map<String, Float> ret = new HashMap<>();
        try {
            int totalDocCount = 0;
            for (Histogram.Bucket bucket : histogram(id,
                                                     "RATING",
                                                     PipelineAggregatorBuilders.cumulativeSum("cum_sum", "rating")).getBuckets()) {
                totalDocCount += Math.toIntExact(bucket.getDocCount());
                SimpleValue cumSumVal = bucket.getAggregations().get("cum_sum");
                ret.put(bucket.getKeyAsString(), Float.parseFloat(cumSumVal.getValueAsString()) / totalDocCount);
            }
        } catch (Exception e) {
            logger.error("Parsing aggregations ", e);
        }
        if (ret.isEmpty()) {
            Stream.of(getDates()).forEach(i -> ret.put(i, 5 * ThreadLocalRandom.current().nextFloat()));
        }
        return ret;
    }

    private InternalDateHistogram histogram(String id, String eventType, PipelineAggregationBuilder optional) {
        AggregationBuilder ab = AggregationBuilders
                .dateHistogram("months")
                .field("instant")
                .dateHistogramInterval(DateHistogramInterval.DAY)
                .format("yyyy-MM-dd")
                .subAggregation(AggregationBuilders.terms("score").field("score"));
        if (optional != null) {
            ab = ab.subAggregation(optional);
        }
        SearchRequestBuilder srb = elastic.client()
                                          .prepareSearch("eventType")
                                          .setTypes("general")
                                          .setQuery(getEventQueryBuilder(id, eventType))
                                          .addAggregation(ab);
        return srb.execute().actionGet().getAggregations().get("months");
    }

    private QueryBuilder getEventQueryBuilder(String serviceId, String typeOf) {
        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.roll(Calendar.MONTH, false);
        return QueryBuilders.boolQuery()
                            .filter(QueryBuilders.termsQuery("service_id", serviceId))
                            .filter(QueryBuilders.rangeQuery("instant").from(c.getTime().getTime()).to(new Date().getTime()))
                            .filter(QueryBuilders.termsQuery("type", typeOf));
    }

    @Override
    public Map<String, Integer> externals(String id) {
        Map<String, Integer> ret = counts(id, "EXTERNAL");
        if (ret.isEmpty()) {
            Stream.of(getDates()).forEach(i -> ret.put(i, ThreadLocalRandom.current().nextInt(0, 9)));
        }
        return ret;
    }

    private Map<String, Integer> counts(String id, String eventType) {
        return histogram(id, eventType, null).getBuckets().stream().collect(
                Collectors.toMap(e -> e.getKeyAsString(), e -> (int) e.getDocCount())
        );
    }

    private static String[] getDates() {
        String[] ret = new String[30];
        Calendar day = Calendar.getInstance();
        for (int i = 0; i < ret.length; i++) {
            day.add(Calendar.DAY_OF_YEAR, -1);
            ret[i] = new SimpleDateFormat("yyyy-MM-dd").format(day.getTime());
        }
        return ret;
    }

    @Override
    public Map<String, Integer> internals(String id) {
        Map<String, Integer> ret = counts(id, "INTERNAL");
        if (ret.isEmpty()) {
            Stream.of(getDates()).forEach(i -> ret.put(i, ThreadLocalRandom.current().nextInt(0, 9)));
        }
        return ret;
    }

    @Override
    public Map<String, Integer> pFavourites(String id) {
        Map<String, Integer> ret = new HashMap<>();
        providerService.getServices(id).stream().forEach(s -> {
            Map<String, Integer> favourites = favourites(s.getId());
            favourites.forEach((k, v) -> {
                ret.putIfAbsent(k, 0);
                ret.put(k, ret.get(k) + v);
            });
        });
        return ret;
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
        Map<String, Integer> ret = new HashMap<>();
        providerService.getServices(id).stream().forEach(s -> {
            Map<String, Integer> internals = internals(s.getId());
            internals.forEach((k, v) -> {
                ret.putIfAbsent(k, 0);
                ret.put(k, ret.get(k) + v);
            });
        });
        return ret;
    }

    @Override
    public Map<String, Integer> favourites(String id) {
        Map<String, Integer> ret = new HashMap<>();
        try {
            int totalDocCountTrue = 0;
            int totalDocCountFalse = 0;
            for (Histogram.Bucket bucket : histogram(id, "FAVOURITE", null).getBuckets()) {
                Terms subTerm = bucket.getAggregations().get("score");
                if (subTerm.getBuckets() != null) {
                    for (Terms.Bucket subBucket : subTerm.getBuckets()) {
                        if (subBucket.getKeyAsNumber().intValue() == 0) {
                            totalDocCountFalse += subBucket.getDocCount();
                        } else if (subBucket.getKeyAsNumber().intValue() == 1) {
                            totalDocCountTrue += subBucket.getDocCount();
                        }
                    }
                }
                if ((totalDocCountTrue - totalDocCountFalse) <= 0) {
                    ret.put(bucket.getKeyAsString(), 0);
                } else {
                    ret.put(bucket.getKeyAsString(), totalDocCountTrue - totalDocCountFalse);
                }
            }
        } catch (Exception e) {
            logger.error("Parsing aggregations ", e);
        }
        if (ret.isEmpty()) {
            Stream.of(getDates()).forEach(i -> ret.put(i, ThreadLocalRandom.current().nextInt(0, 9)));
        }
        return ret;
    }

    @Override
    public Map<String, Integer> pVisits(String id) {
        Map<String, Integer> ret = new HashMap<>();
        providerService.getServices(id).stream().forEach(s -> {
            Map<String, Integer> visits = visits(s.getId());
            visits.forEach((k, v) -> {
                ret.putIfAbsent(k, 0);
                ret.put(k, ret.get(k) + v);
            });
        });
        return ret;
    }

    @Override
    public Map<String, Float> pVisitation(String id) {
        Map<String, Float> ret = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        List<eu.einfracentral.domain.Service> services = providerService.getServices(id);
        final int[] grandTotal = {0};
        services.forEach(service -> {
            final Integer[] total = {0};
            visits(service.getId()).forEach((k, v) -> total[0] += v);
            grandTotal[0] += total[0];
            counts.put(service.getName(), total[0]);
        });
        counts.forEach((k, v) -> {
            ret.put(k, ((float) v) / grandTotal[0]);
        });
        return ret;
    }
}
