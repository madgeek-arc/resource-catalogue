package eu.einfracentral.validators;

import eu.einfracentral.service.ElasticDBValidatorService;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.domain.index.IndexedField;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONObject;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ElasticDBValidator implements ElasticDBValidatorService {

    private static final Logger logger = LogManager.getLogger(ElasticDBValidator.class);

    private final RestHighLevelClient client;
    private final ResourceTypeService resourceTypeService;
    private final ResourceService resourceService;
    private final DataSource dataSource;

    public ElasticDBValidator(RestHighLevelClient client, ResourceTypeService resourceTypeService,
                              ResourceService resourceService, DataSource dataSource) {
        this.client = client;
        this.resourceTypeService = resourceTypeService;
        this.resourceService = resourceService;
        this.dataSource = dataSource;
    }

//    @Scheduled(cron = "0 0 0 * * *") // At midnight every day
//    @Scheduled(fixedDelay = 5 * 60 * 1000)
    private void scheduledValidation() {
        List<String> resourceTypeNames = resourceTypeService.getAllResourceType(0, 100)
                .stream().map(ResourceType::getName).collect(Collectors.toList());
        //TODO: use elastic scroll if we need to check events too (>10k)
        resourceTypeNames.remove("event");
        for (String resourceTypeName : resourceTypeNames) {
            validate(resourceTypeName, true);
            validate(resourceTypeName, false);
        }
    }

    public void validate(String resourceType, boolean validateDBtoElastic) {
        // retrieve from DB
        List<String> databaseResources = fetchResourceIdsFromDB(resourceType);
        // retrieve from Elastic
        List<String> elasticResources = fetchResourceIdsFromElastic(resourceType);

        validateDBAndElasticEntries(databaseResources, elasticResources, resourceType, validateDBtoElastic);
    }

    private List<String> fetchResourceIdsFromDB(String resourceType) {
        List<String> databaseResources = new ArrayList<>();
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        MapSqlParameterSource in = new MapSqlParameterSource();

        String query = "SELECT id FROM " +resourceType+ "_view";

        List<Map<String, Object>> records = namedParameterJdbcTemplate.queryForList(query, in);
        if (records != null && !records.isEmpty()) {
            for (Map<String, Object> record : records) {
                databaseResources.add((String) record.get("id"));
            }
        }
        return databaseResources;
    }

    private List<String> fetchResourceIdsFromElastic(String resourceType) {
        List<String> resourceIds = new ArrayList<>();
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder
                .from(0)
                .size(10000)
                .docValueField("*_id")
                .fetchSource(false)
                .explain(true)
                .query(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("_index", resourceType)));
        searchRequest.source(searchSourceBuilder);

        SearchResponse response;
        try {
            response = client.search(searchRequest, RequestOptions.DEFAULT);
            List<SearchHit> hits = Arrays.stream(response.getHits().getHits()).collect(Collectors.toList());
            for (SearchHit hit : hits) {
                resourceIds.add(hit.getFields().get("_id").getValue());
            }
        } catch (IOException e) {
            logger.error("Error retrieving _id value from Elastic.", e);
        }
        return resourceIds;
    }

    private void validateDBAndElasticEntries(List<String> databaseResources, List<String> elasticResources,
                                             String resourceType, boolean validateDBtoElastic) {
        if (validateDBtoElastic) {
            List<String> missingElasticIds = new ArrayList<>(databaseResources);
            missingElasticIds.removeAll(elasticResources);
            if (!missingElasticIds.isEmpty()) {
                indexMissingElasticIds(missingElasticIds, resourceType);
            } else {
                logger.info("Elastic is consistent with Database on {}", resourceType);
            }
        } else {
            List<String> missingDBIds = new ArrayList<>(elasticResources);
            missingDBIds.removeAll(databaseResources);
            if (!missingDBIds.isEmpty()) {
                //TODO: Add missing resources to DB
                logger.info("Database is missing the following resources {} on {}", missingDBIds, resourceType);
            } else {
                logger.info("Database is consistent with Elastic on {}", resourceType);
            }
        }
    }

    private void indexMissingElasticIds(List<String>  missingElasticIds, String resourceType) {
        logger.info("Adding {} missing indexes for {}", missingElasticIds.size(), resourceType);
        for (String missingElasticId : missingElasticIds) {
            Resource resource = resourceService.getResource(missingElasticId);
            String payload = createDocumentForInsert(resource);

            IndexRequest indexRequest = new IndexRequest(resource.getResourceType().getName());
            indexRequest.id(resource.getId());
            indexRequest.source(payload, XContentType.JSON);
            indexRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

            try {
                client.index(indexRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                throw new ServiceException(e);
            }
        }
    }

    private String createDocumentForInsert(Resource resource) {
        JSONObject jsonObjectField = new JSONObject();
        jsonObjectField.put("id", resource.getId());
        jsonObjectField.put("resourceType", resource.getResourceType().getName());
        jsonObjectField.put("payload", resource.getPayload());
        jsonObjectField.put("payloadFormat", resource.getPayloadFormat());
        jsonObjectField.put("version", resource.getVersion());
        jsonObjectField.put("searchableArea", strip(resource.getPayload(),resource.getPayloadFormat()));
        jsonObjectField.put("modification_date", resource.getModificationDate().getTime());
        //The creation date exists and should not be updated
        if(resource.getCreationDate() != null) {
            jsonObjectField.put("creation_date", resource.getCreationDate().getTime());
        }
        Map<String, IndexField> indexMap = resourceTypeService.getResourceTypeIndexFields(
                        resource.getResourceType().getName()).
                stream().collect(Collectors.toMap(IndexField::getName, p->p)
                );
        if (resource.getIndexedFields() != null) {
            for (IndexedField<?> field : resource.getIndexedFields()) {
                if(!indexMap.get(field.getName()).isMultivalued()) {
                    for (Object value : field.getValues()) {
                        String fieldType = indexMap.get(field.getName()).getType();
                        if(fieldType.equals("java.lang.String")){
                            jsonObjectField.put(field.getName(), value);
                        }else if(fieldType.equals("java.lang.Integer")){
                            jsonObjectField.put(field.getName(),value);
                        }else if(fieldType.equals("java.lang.Long")){
                            jsonObjectField.put(field.getName(),value);
                        }else if(fieldType.equals("java.lang.Float")){
                            jsonObjectField.put(field.getName(),value);
                        }else if(fieldType.equals("java.util.Date")){
                            Date date = (Date) value;
                            jsonObjectField.put(field.getName(), date.getTime());
                        }else if (fieldType.equals("java.lang.Boolean")){
                            jsonObjectField.put(field.getName(),value);
                        }
                    }
                } else {
                    List<Object> values = new ArrayList<>(field.getValues());
                    jsonObjectField.put(field.getName(),values);

                }
            }
        }
        return jsonObjectField.toString();
    }

    private static String strip(String input, String format) {
        if ( "xml".equals(format)) {
            return input.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ");
        } else if ("json".equals(format)) {
            return input;
        } else {
            throw new ServiceException("Invalid format type, supported are json and xml");
        }
    }
}
