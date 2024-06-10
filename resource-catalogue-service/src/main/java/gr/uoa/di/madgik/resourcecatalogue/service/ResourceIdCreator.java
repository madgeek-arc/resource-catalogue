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

    @Value("${pid.prefix.services}")
    private String servicesPrefix;
    @Value("${pid.prefix.tools}")
    private String toolsPrefix;
    @Value("${pid.prefix.trainings}")
    private String trainingsPrefix;
    @Value("${pid.prefix.providers}")
    private String providersPrefix;
    @Value("${pid.prefix.ifguidelines}")
    private String ifguidelinesPrefix;

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
            case "service", "pending_service" -> servicesPrefix;
            case "tool" -> toolsPrefix;
            case "training_resource" -> trainingsPrefix;
            case "provider", "pending_provider" -> providersPrefix;
            case "interoperability_record" -> ifguidelinesPrefix;
            // non PID related
            case "catalogue" -> "cat";
            case "configuration_template" -> "con";
            case "configuration_template_instance" -> "cti";
            case "datasource" -> "dat";
            case "helpdesk" -> "hel";
            case "monitoring" -> "mon";
            case "resource_interoperability_record" -> "rir";
            case "vocabulary_curation" -> "cur";
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
