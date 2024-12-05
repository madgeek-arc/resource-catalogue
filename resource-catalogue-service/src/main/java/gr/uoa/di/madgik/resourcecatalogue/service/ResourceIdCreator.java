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

    @Value("${pid.test:false}")
    private boolean pidTest;

    private final SearchService searchService;

    @Value("${pid.services.prefix}")
    private String servicesPrefix;
    @Value("${pid.tools.prefix}")
    private String toolsPrefix;
    @Value("${pid.trainings.prefix}")
    private String trainingsPrefix;
    @Value("${pid.providers.prefix}")
    private String providersPrefix;
    @Value("${pid.interoperability-frameworks.prefix}")
    private String guidelinesPrefix;

    @Value("${configuration-templates.prefix}")
    private String configurationTemplatesPrefix;
    @Value("${configuration-template-instances.prefix}")
    private String configurationTemplateInstancesPrefix;
    @Value("${datasources.prefix}")
    private String datasourcesPrefix;
    @Value("${helpdesks.prefix}")
    private String helpdesksPrefix;
    @Value("${monitorings.prefix}")
    private String monitoringsPrefix;
    @Value("${resource-interoperability-records.prefix}")
    private String resourceInteroperabilityRecordsPrefix;
    @Value("${vocabulary-curations.prefix}")
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
        if (pidTest) {
            ff.setResourceType("resourceTypes");
        } else {
            ff.setResourceType(resourceType);
        }
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
