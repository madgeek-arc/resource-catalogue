package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.service.SearchService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ResourceIdCreator implements IdCreator {

    private final SearchService searchService;

    @Value("${prefix.services}")
    private String servicesPrefix;
    @Value("${prefix.tools}")
    private String toolsPrefix;
    @Value("${prefix.trainings}")
    private String trainingsPrefix;
    @Value("${prefix.providers}")
    private String providersPrefix;
    @Value("${prefix.guidelines}")
    private String guidelinesPrefix;
    @Value("${prefix.catalogues}")
    private String cataloguesPrefix;
    @Value("${prefix.configurationTemplates}")
    private String configurationTemplatesPrefix;
    @Value("${prefix.configurationTemplateInstances}")
    private String configurationTemplateInstancesPrefix;
    @Value("${prefix.datasources}")
    private String datasourcesPrefix;
    @Value("${prefix.helpdesks}")
    private String helpdesksPrefix;
    @Value("${prefix.monitorings}")
    private String monitoringsPrefix;
    @Value("${prefix.resourceInteroperabilityRecords}")
    private String resourceInteroperabilityRecordsPrefix;
    @Value("${prefix.vocabularyCurations}")
    private String vocabularyCurationsPrefix;

    public ResourceIdCreator(SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public String generate(String resourceType) {
        String prefix = createPrefix(resourceType);
        String id = prefix + "/" + randomGenerator();
        while (searchIdExists(id, resourceType)) {
            id = prefix + "/" + randomGenerator();
        }
        return id;
    }

    private String createPrefix(String resourceType) {
        return switch (resourceType) {
            // PID related
            case "service", "draft_service" -> servicesPrefix;
            case "tool" -> toolsPrefix;
            case "training_resource", "draft_training_resource" -> trainingsPrefix;
            case "provider", "draft_provider" -> providersPrefix;
            case "interoperability_record", "draft_interoperability_record" -> guidelinesPrefix;
            // non PID related
            case "catalogue" -> cataloguesPrefix;
            case "configuration_template" -> configurationTemplatesPrefix;
            case "configuration_template_instance" -> configurationTemplateInstancesPrefix;
            case "datasource" -> datasourcesPrefix;
            case "helpdesk" -> helpdesksPrefix;
            case "monitoring" -> monitoringsPrefix;
            case "resource_interoperability_record" -> resourceInteroperabilityRecordsPrefix;
            case "vocabulary_curation" -> vocabularyCurationsPrefix;
            default -> "non";
        };
    }

    private String randomGenerator() {
        return RandomStringUtils.randomAlphanumeric(6);
    }

    public boolean searchIdExists(String id, String resourceType) {
        FacetFilter ff = new FacetFilter();
        ff.setResourceType(resourceType);
        ff.addFilter("resource_internal_id", id);
        Paging<?> resources = searchService.search(ff);
        return resources.getTotal() > 0;
    }

    @Override
    public String sanitizeString(String input) {
        return StringUtils
                .stripAccents(input)
                .replaceAll("[\\n\\t\\s]+", " ")
                .replaceAll("\\s+$", "")
                .replaceAll("[^a-zA-Z0-9\\s\\-_/]+", "")
                .replaceAll("[/\\s]+", "_")
                .toLowerCase();
    }
}
