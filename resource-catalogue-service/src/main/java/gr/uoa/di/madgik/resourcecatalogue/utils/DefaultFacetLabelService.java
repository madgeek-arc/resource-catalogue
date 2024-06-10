package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.registry.domain.Facet;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Value;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import org.apache.commons.collections.list.TreeList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

@Order(Ordered.LOWEST_PRECEDENCE)
@Component
public class DefaultFacetLabelService implements FacetLabelService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultFacetLabelService.class);
    private final ProviderService providerService;
    private final VocabularyService vocabularyService;

    @org.springframework.beans.factory.annotation.Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    DefaultFacetLabelService(ProviderService providerService,
                             VocabularyService vocabularyService) {
        this.providerService = providerService;
        this.vocabularyService = vocabularyService;
    }

    @Override
    public List<Facet> generateLabels(List<Facet> facets) {
        return createLabels(facets);
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
                                logger.debug(e.getMessage(), e);
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
