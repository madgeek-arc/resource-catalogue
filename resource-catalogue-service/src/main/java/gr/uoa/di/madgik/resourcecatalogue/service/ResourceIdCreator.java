package gr.uoa.di.madgik.resourcecatalogue.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class ResourceIdCreator implements IdCreator {

    @Override
    public String generate(String resourceType) {
        String prefix = resourceType.substring(0, 3);
        return prefix + randomGenerator();
    }

    private String randomGenerator() {
        return RandomStringUtils.random(7, true, true);
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
