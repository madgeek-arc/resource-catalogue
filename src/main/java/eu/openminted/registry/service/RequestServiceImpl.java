package eu.openminted.registry.service;

import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Occurencies;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import eu.openminted.registry.domain.*;
import org.apache.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.UnknownHostException;
import java.util.*;

@Service("requestService")
public class RequestServiceImpl implements RequestService {

    @Autowired
    SearchService searchService;

    final private static Logger logger = Logger.getLogger(RequestServiceImpl.class);

    final private static String RESOURCE_ALIAS = "resourceTypes";

    private static Map<String, String> labels = new LinkedHashMap<>();
    private static String[] facets = new String[]{
            "resourceType", "mediaType", "rights",
            "mimeType", "dataFormatSpecific", "licence"
    };

    static {
        labels.put("resourceType", "Resource Type");
        labels.put("licence", "Licence");
        labels.put("language", "Language");
        labels.put("mediaType", "Media Type");
        labels.put("rights", "Rights");
        labels.put("mimeType", "Mime Type");
        labels.put("dataFormatSpecific", "Data format specific");
    }

    public Browsing getResponseByFiltersElastic(FacetFilter filter) {

        List<Order<BaseMetadataRecord>> result = new ArrayList<>();

        int totalNumber = 0;

        filter.setResourceType(RESOURCE_ALIAS);
        filter.setBrowseBy(Arrays.asList(facets));
        Occurencies overall = new Occurencies();

        try {
            Paging paging = searchService.search(filter);
            result = createResults(paging);
            totalNumber += paging.getTotal();
            overall = paging.getOccurencies();

        } catch (ServiceException e) {
            throw new ServiceException(e.getMessage());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        List<Facet> facetsCollection = createFacetCollection(overall);

        Browsing browsing = new Browsing(totalNumber, filter.getFrom(), filter.getFrom() + result.size(), result, facetsCollection);
        return browsing;

    }

    /**
     * Counts the total number of Documents per Facet
     * @param overall
     * @return a List of facets.
     */
    static List<Facet> createFacetCollection(Occurencies overall) {
        List<Facet> facetsCollection = new ArrayList<>();

        for (String label : labels.keySet()) {
            Facet singleFacet = new Facet();

            singleFacet.setField(label + "");
            singleFacet.setLabel(labels.get(label));

            List<Value> values = new ArrayList<>();
            Map<String, Integer> subMap = overall.getValues().get(label);
            if (subMap == null)
                continue;
            for (Map.Entry<String, Integer> pair2 : subMap.entrySet()) {
                Value value = new Value();

                value.setValue(pair2.getKey() + "");
                value.setCount(Integer.parseInt(pair2.getValue() + ""));

                values.add(value);
            }

            Collections.sort(values);
            Collections.reverse(values);
            singleFacet.setValues(values);

            if (singleFacet.getValues().size() > 0)
                facetsCollection.add(singleFacet);
        }
        return facetsCollection;
    }


    /**
     * Deserializes according to resourceType the results from the indexer.
     * @param paging the indexer result object
     * @return a Result object
     */
    static List<Order<BaseMetadataRecord>> createResults(Paging paging) {
        List<Order<BaseMetadataRecord>> parsedXML = new ArrayList<>();
        if (paging != null) {
            int pos = 0;
            for(Object resourceObj :  paging.getResults()) {
                Resource resource = (Resource) resourceObj;
                BaseMetadataRecord temp = null;
                switch (resource.getResourceType()) {
                    case "corpus": temp = Utils.serialize(resource, Corpus.class); break;
                    case "component":temp = Utils.serialize(resource, Component.class); break;
                    case "lexical": temp = Utils.serialize(resource,Lexical.class); break;
                    case "language": temp = Utils.serialize(resource,LanguageDescription.class); break;
                    default : logger.warn("Unsupported resourceType [" + resource.getResourceType()+"]");
                }
                parsedXML.add(new Order<>(pos,temp));
                pos++;
            }
        }
        return parsedXML;
    }
}
