package gr.uoa.di.madgik.resourcecatalogue.service;

import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.service.IdCreator;
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
        return sanitizeString(catalogueId);
    }

    @Override
    public String createProviderId(Provider provider) {
        String providerId;
        if (provider.getAbbreviation() != null && !"".equals(provider.getAbbreviation()) && !"null".equals(provider.getAbbreviation())) {
            providerId = provider.getAbbreviation();
        } else {
            throw new ValidationException("Provider must have an abbreviation.");
        }
        return sanitizeString(providerId);
    }

    @Override
    public String createServiceId(ServiceBundle serviceBundle) {
        if (serviceBundle.getService().getResourceOrganisation() == null || serviceBundle.getService().getResourceOrganisation().equals("")) {
            throw new ValidationException("Resource must have a Resource Organisation.");
        }
        String serviceId;
        if (serviceBundle.getService().getAbbreviation() != null && !"".equals(serviceBundle.getService().getAbbreviation()) && !"null".equals(serviceBundle.getService().getAbbreviation())) {
            serviceId = serviceBundle.getService().getAbbreviation();
        } else {
            throw new ValidationException("Resource must have an abbreviation.");
        }
        String provider = serviceBundle.getService().getResourceOrganisation();
        return String.format("%s.%s", provider, sanitizeString(serviceId));
    }

    public String createTrainingResourceId(TrainingResourceBundle trainingResourceBundle) throws NoSuchAlgorithmException {
        String resourceOrganisation = trainingResourceBundle.getTrainingResource().getResourceOrganisation();
        String trainingResourceTitle = trainingResourceBundle.getTrainingResource().getTitle();
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] messageDigest = md.digest(trainingResourceTitle.getBytes());
        BigInteger no = new BigInteger(1, messageDigest);
        String hashtext = no.toString(16);
        while (hashtext.length() < 32) {
            hashtext = "0" + hashtext;
        }
        return resourceOrganisation + "." + hashtext;
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
