package eu.einfracentral.service;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ValidationException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class SimpleIdCreator implements IdCreator {

    SimpleIdCreator() {
    }

    @Override
    public String createProviderId(Provider provider) {
        String providerId;
        if (provider.getAbbreviation() != null && !"".equals(provider.getAbbreviation()) && !"null".equals(provider.getAbbreviation())) {
            providerId = provider.getAbbreviation();
        } else {
            throw new ValidationException("Provider must have an abbreviation.");
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
            } else {
                throw new ValidationException("Catalogue must have an abbreviation.");
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
    public String createServiceId(ResourceBundle<?> resource) {
        if (resource.getPayload().getResourceOrganisation() == null || resource.getPayload().getResourceOrganisation().equals("")) {
            throw new ValidationException("Resource must have a Resource Organisation.");
        }
        String serviceId;
        if (resource.getPayload().getAbbreviation() != null && !"".equals(resource.getPayload().getAbbreviation()) && !"null".equals(resource.getPayload().getAbbreviation())) {
            serviceId = resource.getPayload().getAbbreviation();
        } else {
            throw new ValidationException("Resource must have an abbreviation.");
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

    public String createDatasourceId(ResourceBundle<?> resource) throws NoSuchAlgorithmException {
        String resourceOrganisation = resource.getPayload().getResourceOrganisation();
        String datasourceName = resource.getPayload().getName();
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] messageDigest = md.digest(datasourceName.getBytes());
        BigInteger no = new BigInteger(1, messageDigest);
        String hashtext = no.toString(16);
        while (hashtext.length() < 32) {
            hashtext = "0" + hashtext;
        }
        return resourceOrganisation+"."+hashtext;
    }

    public String createInteroperabilityRecordId(InteroperabilityRecord interoperabilityRecord) throws NoSuchAlgorithmException {
        String identifier = interoperabilityRecord.getIdentifierInfo().getIdentifier() + '.' + interoperabilityRecord.getIdentifierInfo().getIdentifierType();
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] messageDigest = md.digest(identifier.getBytes());
        BigInteger no = new BigInteger(1, messageDigest);
        String hashtext = no.toString(16);
        while (hashtext.length() < 32) {
            hashtext = "0" + hashtext;
        }
        return hashtext;
    }

    @Override
    public String reformatId(String toBeReformatted) {
        return StringUtils
                .stripAccents(toBeReformatted)
                .replaceAll("[\\n\\t\\s]+", " ")
                .replaceAll("\\s+$", "")
                .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                .replace(" ", "_")
                .toLowerCase();

    }
}
