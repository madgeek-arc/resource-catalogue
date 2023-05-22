package eu.einfracentral.utils;

import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.domain.Vocabulary;
import eu.einfracentral.registry.service.ProviderService;
import eu.einfracentral.registry.service.VocabularyService;
import eu.openminted.registry.core.domain.Facet;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Value;
import org.apache.commons.collections.list.TreeList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class FacetLabelService {

    private static final Logger logger = LogManager.getLogger(FacetLabelService.class);
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final VocabularyService vocabularyService;
    private final RestHighLevelClient client;

    @org.springframework.beans.factory.annotation.Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    @Autowired
    FacetLabelService(ProviderService<ProviderBundle, Authentication> providerService,
                      VocabularyService vocabularyService,
                      RestHighLevelClient client) {
        this.providerService = providerService;
        this.vocabularyService = vocabularyService;
        this.client = client;
    }

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
            try{
                String s1;
                s1 = s.substring(0, 1).toUpperCase() + s.substring(1);
                joiner.add(s1);
            } catch (IndexOutOfBoundsException e){
                return str;
            }
        }
        return joiner.toString();
    }

    static class IdName {

        private String id;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    String getLabelElseKeepValue(String value, Map<String, String> labels) {
        String ret = labels.get(value);
        if (ret == null) {
            ret = toProperCase(toProperCase(value, "-", "-"), "_", " ");
        }
        return ret;
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    public List<Facet> createLabels(List<Facet> facets) {
        List<Facet> enrichedFacets = new TreeList(); // unchecked warning here
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
//        ff.addFilter("active", "true");
        // TODO: get all final providers (after deduplication process)
        List<ProviderBundle> allProviders = providerService.getAll(ff, null).getResults();
        Map<String, String> providerNames = new TreeMap<>();
        allProviders.forEach(p -> providerNames.putIfAbsent(p.getId(), p.getProvider().getName()));

        Map<String, Vocabulary> allVocabularies = vocabularyService.getVocabulariesMap();

        Facet superCategories;
        Facet categories;
        Facet scientificDomains;

        for (Facet facet : facets) {
            if (facet.getField().equals("subcategories")) {
                categories = createCategoriesFacet(facet);
                superCategories = createSupercategoriesFacet(categories);

                enrichedFacets.add(superCategories);
                enrichedFacets.add(categories);
            }
            if (facet.getField().equals("scientific_subdomains")) {
                scientificDomains = createScientificDomainsFacet(facet);
                enrichedFacets.add(scientificDomains);
            }
            for (Value value : facet.getValues()) {

                switch (facet.getField()) {
                    case "resource_providers":
                    case "resource_organisation":
                        value.setLabel(providerNames.get(value.getValue()));
                        break;

                    default:
                        if (allVocabularies.containsKey(value.getValue())) {
                            value.setLabel(allVocabularies.get(value.getValue()).getName());
                        } else {
                            //TODO: Find a better way to prettify Labels
                            try {
                                value.setLabel(toProperCase(toProperCase(value.getValue(), "-", "-"), "_", " "));
                            } catch (StringIndexOutOfBoundsException e) {
                                logger.debug(e);
                            }
                        }
                }
            }
        }
        enrichedFacets.addAll(facets);

        return enrichedFacets;
    }

    @Deprecated
    Facet createCategoriesFacet(Facet subcategories) {
        List<Value> categoriesValues = new ArrayList<>();

        Map<String, Vocabulary> categoriesMap = new TreeMap<>();

        for (Value value : subcategories.getValues()) {
            Vocabulary parent = vocabularyService.getParent(value.getValue());
            if (parent != null) {
                categoriesMap.putIfAbsent(parent.getId(), parent);
            }
        }

        for (Vocabulary category : categoriesMap.values()) {
            Value value = new Value();
            value.setValue(category.getId());
            value.setLabel(category.getName());
            categoriesValues.add(value);
        }

        Facet categories = new Facet();
        categories.setField("categories");
        categories.setLabel("Categories");
        categories.setValues(categoriesValues);
        return categories;
    }

    @Deprecated
    Facet createSupercategoriesFacet(Facet categories) {
        List<Value> superCategoriesValues = new ArrayList<>();

        Map<String, Vocabulary> categoriesMap = new TreeMap<>();

        for (Value value : categories.getValues()) {
            Vocabulary parent = vocabularyService.getParent(value.getValue());
            if (parent != null) {
                categoriesMap.putIfAbsent(parent.getId(), parent);
            }
        }

        for (Vocabulary category : categoriesMap.values()) {
            Value value = new Value();
            value.setValue(category.getId());
            value.setLabel(category.getName());
            superCategoriesValues.add(value);
        }

        Facet superCategories = new Facet();
        superCategories.setField("supercategories");
        superCategories.setLabel("Supercategories");
        superCategories.setValues(superCategoriesValues);
        return superCategories;
    }

    @Deprecated
    Facet createScientificDomainsFacet(Facet scientificSubdomains) {
        List<Value> scientificDomainsValues = new ArrayList<>();

        Map<String, Vocabulary> categoriesMap = new TreeMap<>();

        for (Value value : scientificSubdomains.getValues()) {
            Vocabulary parent = vocabularyService.getParent(value.getValue());
            if (parent != null) {
                categoriesMap.putIfAbsent(parent.getId(), parent);
            }
        }

        for (Vocabulary category : categoriesMap.values()) {
            Value value = new Value();
            value.setValue(category.getId());
            value.setLabel(category.getName());
            scientificDomainsValues.add(value);
        }

        Facet scientificDomains = new Facet();
        scientificDomains.setField("scientific_domains");
        scientificDomains.setLabel("Scientific Domains");
        scientificDomains.setValues(scientificDomainsValues);
        return scientificDomains;
    }
}
