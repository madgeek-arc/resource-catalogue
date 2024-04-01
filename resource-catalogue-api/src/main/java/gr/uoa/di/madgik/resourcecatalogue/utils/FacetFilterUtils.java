package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;

public class FacetFilterUtils {

    public static final String SEARCH_FIELDS = "searchFields";
    public static final String SEARCHABLE_AREA = "searchableArea";

    private static final Logger logger = LogManager.getLogger(FacetFilterUtils.class);

    private FacetFilterUtils() {
    }

    // Gets all given filters
    public static Map<String, List<Object>> getFacetFilterFilters(FacetFilter ff) {
        Map<String, Object> filters = new LinkedHashMap<>(ff.getFilter());
        Map<String, List<Object>> allFilters = new LinkedHashMap<>();

        // fill the variable with the rest of the filters
        for (Map.Entry<String, Object> ffEntry : filters.entrySet()) {
            if (ffEntry.getValue() instanceof List) {
                allFilters.put(ffEntry.getKey(), (List) ffEntry.getValue());
            } else {
                allFilters.put(ffEntry.getKey(), Collections.singletonList(ffEntry.getValue().toString()));
            }
        }

        return allFilters;
    }

    // Creates a Query consisted of all given filters and keywords
    public static String createQuery(Map<String, List<Object>> filters, String keyword) {
        List<Object> searchFields = filters.remove(FacetFilterUtils.SEARCH_FIELDS);
        if (searchFields == null || searchFields.isEmpty()) {
            searchFields = Collections.singletonList(FacetFilterUtils.SEARCHABLE_AREA);
        }
        final List<Object> fields = searchFields;
        StringBuilder query = new StringBuilder();

        if (keyword != null && !keyword.replaceAll(" ", "").equals("")) {
            String keywordQuery;
            List<String> searchKeywords = Arrays.asList(keyword.split(" "));
            List<String> allSearchKeywords = new ArrayList<>();
            // filter search keywords, trim whitespace and create search statements
            for (Object f : fields) {
                allSearchKeywords.addAll(searchKeywords
                        .stream()
                        .map(k -> k.replaceAll(" ", ""))
                        .filter(k -> !k.equals(""))
                        .map(k -> String.format("%s=%s", f, k))
                        .collect(Collectors.toList()));
            }
            keywordQuery = String.join(" OR ", allSearchKeywords);
            query.append(String.format("( %s )", keywordQuery));

            if (!filters.isEmpty()) {
                query.append(" AND ");
            }
        }

        for (Iterator iter = filters.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<String, List<Object>> filter = (Map.Entry<String, List<Object>>) iter.next();
            List<String> entries = new ArrayList<>();
            filter.getValue().forEach(e -> entries.add(String.format("%s=%s", filter.getKey(), e)));
            if (entries.size() > 1) {
                query.append(String.format("( %s )", String.join(" OR ", entries)));
            } else { // this is important to skip adding parentheses when we have zero or only 1 filter
                query.append(String.join("", entries));
            }

            if (iter.hasNext()) {
                query.append(" AND ");
            }
        }

        return query.toString();
    }

    public static FacetFilter createFacetFilter(Map<String, Object> params) {
        FacetFilter ff = new FacetFilter();
        ff.setKeyword(params.get("query") != null ? (String) params.remove("query") : "");
        ff.setFrom(params.get("from") != null ? Integer.parseInt((String) params.remove("from")) : 0);
        ff.setQuantity(params.get("quantity") != null ? Integer.parseInt((String) params.remove("quantity")) : 10);
        Map<String, Object> sort = new HashMap<>();
        Map<String, Object> order = new HashMap<>();
        String orderDirection = params.get("order") != null ? (String) params.remove("order") : "asc";
        String orderField = params.get("orderField") != null ? (String) params.remove("orderField") : null;
        if (orderField != null) {
            order.put("order", orderDirection);
            sort.put(orderField, order);
            ff.setOrderBy(sort);
        }
        ff.setFilter(params);
        return ff;
    }

    public static FacetFilter createMultiFacetFilter(Map<String, List<Object>> params) {
        MultiValueMap<String, Object> requestParams = new LinkedMultiValueMap<>(params);
        return createFacetFilter(requestParams);
    }

    public static FacetFilter createFacetFilter(MultiValueMap<String, Object> params) {
        logger.debug("Request params: {}", params);
        FacetFilter ff = new FacetFilter();
        ff.setKeyword(params.get("query") != null ? (String) params.remove("query").get(0) : "");
        ff.setFrom(params.get("from") != null ? Integer.parseInt((String) params.remove("from").get(0)) : 0);
        ff.setQuantity(params.get("quantity") != null ? Integer.parseInt((String) params.remove("quantity").get(0)) : 10);
        Map<String, Object> sort = new HashMap<>();
        Map<String, Object> order = new HashMap<>();
        String orderDirection = params.get("order") != null ? (String) params.remove("order").get(0) : "asc";
        String orderField = params.get("orderField") != null ? (String) params.remove("orderField").get(0) : null;
        if (orderField != null) {
            order.put("order", orderDirection);
            sort.put(orderField, order);
            ff.setOrderBy(sort);
        }
        if (!params.isEmpty()) {
            for (Map.Entry<String, List<Object>> filter : params.entrySet()) {
                ff.addFilter(filter.getKey(), filter.getValue());
            }
        }
        return ff;
    }
}
