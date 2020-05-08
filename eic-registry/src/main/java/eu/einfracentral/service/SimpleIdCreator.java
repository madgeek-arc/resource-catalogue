package eu.einfracentral.service;

import eu.einfracentral.domain.Provider;
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
            if (provider.getAbbreviation() != null && !"".equals(provider.getAbbreviation())) {
                providerId = provider.getAbbreviation();
            } else if (provider.getName() != null && !"".equals(provider.getName())) {
                providerId = provider.getName();
            } else {
                throw new ValidationException("Provider must have an acronym or name.");
            }
        } else {
            providerId = provider.getId();
        }
        return StringUtils
                .stripAccents(providerId)
                .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                .replace(" ", "_");

    }

    @Override
    public String createServiceId(eu.einfracentral.domain.Service service) {
        if (service.getResourceOrganisation() == null || service.getResourceOrganisation().equals("")) {
            throw new ValidationException("Service must have a Service Organisation.");
        }
        if (service.getName() == null || service.getName().equals("")) {
            throw new ValidationException("Service must have a Name.");
        }
        String provider = service.getResourceOrganisation();
        return String.format("%s.%s", provider, StringUtils
                .stripAccents(service.getName())
                .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                .replace(" ", "_")
                .toLowerCase());
    }
}
