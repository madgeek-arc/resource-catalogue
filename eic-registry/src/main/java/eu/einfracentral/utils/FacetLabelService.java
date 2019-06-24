package eu.einfracentral.utils;

import eu.einfracentral.domain.NewVocabulary;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.registry.service.NewVocabularyService;
import eu.einfracentral.registry.service.ProviderService;
import eu.openminted.registry.core.domain.Facet;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Value;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FacetLabelService {

    private static final Logger logger = LogManager.getLogger(FacetLabelService.class);
    private ProviderService<Provider, Authentication> providerService;
    private NewVocabularyService vocabularyService;

    @Autowired
    FacetLabelService(ProviderService<Provider, Authentication> providerService, NewVocabularyService vocabularyService) {
        this.providerService = providerService;
        this.vocabularyService = vocabularyService;
    }

    String toProperCase(String str, String delimiter, String newDelimiter) {
        return Arrays.stream(str.split(delimiter)).map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                .collect(Collectors.joining(newDelimiter));
    }

    public void createLabels(List<Facet> facets) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10000);
        Map<String, String> providerNames = providerService.getAll(ff, null)
                .getResults()
                .stream().collect(Collectors.toMap(Provider::getId, Provider::getName));
        Map<String, NewVocabulary> allVocabularies = vocabularyService.getVocabulariesMap();

        for (Facet facet : facets) {
            for (Value value : facet.getValues()) {
                switch (facet.getField()) {
                    case "providers":
                        value.setLabel(providerNames.get(value.getValue()));
                        break;

                    default:
                        if (allVocabularies.containsKey(value.getValue())) {
                            value.setLabel(allVocabularies.get(value.getValue()).getName());
                        } else {
                            value.setLabel(toProperCase(toProperCase(value.getValue(), "-", "-"), "_", " "));
                        }
                }
            }
        }
    }
}
