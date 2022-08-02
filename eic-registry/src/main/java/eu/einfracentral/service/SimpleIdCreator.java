package eu.einfracentral.service;

import eu.einfracentral.domain.Catalogue;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ResourceBundle;
import eu.einfracentral.exception.ValidationException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class SimpleIdCreator implements IdCreator {

    SimpleIdCreator() {
    }

    @Override
    public String createProviderId(Provider provider) {
        String providerId;
        if (provider.getId() == null || "".equals(provider.getId())) {
            if (provider.getAbbreviation() != null && !"".equals(provider.getAbbreviation()) && !"null".equals(provider.getAbbreviation())) {
                providerId = provider.getAbbreviation();
            } else if (provider.getName() != null && !"".equals(provider.getName()) && !"null".equals(provider.getName())) {
                providerId = provider.getName();
            } else {
                throw new ValidationException("Provider must have an abbreviation or name.");
            }
        } else {
            providerId = provider.getId();
        }
        return StringUtils
                .stripAccents(providerId)
                .replaceAll("[\\n\\t\\s]+", " ")
                .replaceAll("\\s+$", "")
                .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                .replace(" ", "_")
                .toLowerCase();

    }

    @Override
    public String createCatalogueId(Catalogue catalogue) {
        String catalogueId;
        if (catalogue.getId() == null || "".equals(catalogue.getId())) {
            if (catalogue.getAbbreviation() != null && !"".equals(catalogue.getAbbreviation()) && !"null".equals(catalogue.getAbbreviation())) {
                catalogueId = catalogue.getAbbreviation();
            } else if (catalogue.getName() != null && !"".equals(catalogue.getName()) && !"null".equals(catalogue.getName())) {
                catalogueId = catalogue.getName();
            } else {
                throw new ValidationException("Catalogue must have an abbreviation or name.");
            }
        } else {
            catalogueId = catalogue.getId();
        }
        return StringUtils
                .stripAccents(catalogueId)
                .replaceAll("[\\n\\t\\s]+", " ")
                .replaceAll("\\s+$", "")
                .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                .replace(" ", "_")
                .toLowerCase();

    }

    @Override
    public String createResourceId(ResourceBundle<?> resource) {
        if (resource.getPayload().getResourceOrganisation() == null || resource.getPayload().getResourceOrganisation().equals("")) {
            throw new ValidationException("Resource must have a Resource Organisation.");
        }
        String serviceId;
        if (resource.getPayload().getId() == null || "".equals(resource.getPayload().getId())) {
            if (resource.getPayload().getAbbreviation() != null && !"".equals(resource.getPayload().getAbbreviation()) && !"null".equals(resource.getPayload().getAbbreviation())) {
                serviceId = resource.getPayload().getAbbreviation();
            } else if (resource.getPayload().getName() != null && !"".equals(resource.getPayload().getName()) && !"null".equals(resource.getPayload().getName())) {
                serviceId = resource.getPayload().getName();
            } else {
                throw new ValidationException("Resource must have an abbreviation or name.");
            }
        } else {
            serviceId = resource.getPayload().getId();
        }
        String provider = resource.getPayload().getResourceOrganisation();
        return String.format("%s.%s", provider, StringUtils
                .stripAccents(serviceId)
                .replaceAll("[\n\t\\s]+", " ")
                .replaceAll("\\s+$", "")
                .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                .replace(" ", "_")
                .toLowerCase());
    }

    @Override
    public String createHostingLegalEntityId(String providerName) {
        return StringUtils
                .stripAccents(providerName)
                .replaceAll("[\\n\\t\\s]+", " ")
                .replaceAll("\\s+$", "")
                .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                .replace(" ", "_")
                .toLowerCase();

    }
}
