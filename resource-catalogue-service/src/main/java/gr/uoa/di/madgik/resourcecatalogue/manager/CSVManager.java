package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.CSVService;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@org.springframework.stereotype.Service
public class CSVManager implements CSVService {

    public String listProvidersToCSV(List<ProviderBundle> list) {
        List<Provider> providers = new ArrayList<>();
        for (ProviderBundle providerBundle : list) {
            providers.add(providerBundle.getProvider());
        }
        providers.sort(Comparator.comparing(Provider::getId));

        StringBuilder csv = new StringBuilder();
        csv.append("id,name\n"); // CSV header

        for (Provider provider : providers) {
            csv.append(formatCSVField(provider.getId())).append(",");
            csv.append(formatCSVField(provider.getName())).append("\n");
        }

        return csv.toString();
    }

    public String listServicesToCSV(List<ServiceBundle> list) {
        List<Service> services = new ArrayList<>();
        for (ServiceBundle serviceBundle : list) {
            services.add(serviceBundle.getService());
        }
        services.sort(Comparator.comparing(Service::getId));

        StringBuilder csv = new StringBuilder();
        csv.append("id,name\n"); // CSV header

        for (Service service : services) {
            csv.append(formatCSVField(service.getId())).append(",");
            csv.append(formatCSVField(service.getName())).append("\n");
        }

        return csv.toString();
    }

    public String listVocabulariesToCSV(List<Vocabulary> list) {
        list.sort(Comparator.comparing(Vocabulary::getId));

        StringBuilder csv = new StringBuilder();
        csv.append("id,name,description,parentId,type,extras\n"); // CSV header

        for (Vocabulary vocabulary : list) {
            csv.append(formatCSVField(vocabulary.getId())).append(",");
            csv.append(formatCSVField(vocabulary.getName())).append(",");
            csv.append(formatCSVField(vocabulary.getDescription())).append(",");
            csv.append(formatCSVField(vocabulary.getParentId())).append(",");

            csv.append(formatCSVField(vocabulary.getType())).append("\n");
        }

        return csv.toString();
    }

    public long generateTimestampFromDate(String date) {
        return validateDate(date);
    }

    private long validateDate(String date) {
        long timestamp;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate localDate = LocalDate.parse(date, formatter);
            timestamp = localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Please use yyyy-MM-dd.");
        }
        return timestamp;
    }

    public void computeApprovedServicesBeforeTimestampAndGenerateCSV(long timestamp,
                                                                     List<ProviderBundle> providers,
                                                                     List<ServiceBundle> services,
                                                                     HttpServletResponse response) throws IOException {
        Map<String, String> providerIdToCountry = new TreeMap<>();
        Map<String, String> providerIdToName = new TreeMap<>();
        Map<String, Integer> providerToServiceCountApprovedBeforeTimestamp = new TreeMap<>();
        List<String> providerOfServicesWithMalformedLoggingInfo = new ArrayList<>();
        for (ProviderBundle provider : providers) {
            providerIdToName.put(provider.getId(), provider.getProvider().getName());
            providerIdToCountry.put(provider.getId(), provider.getProvider().getLocation().getCountry());
        }
        for (ServiceBundle service : services) {
            boolean approvedFound = false;
            boolean rejectedFound = false;
            String resourceOrganisation = service.getService().getResourceOrganisation();
            List<LoggingInfo> loggingInfoList = service.getLoggingInfo();
            for (LoggingInfo loggingInfo : loggingInfoList) {
                try {
                    if (loggingInfo.getActionType().equals("rejected")) {
                        rejectedFound = true;
                    }
                    if (loggingInfo.getActionType().equals("approved")) {
                        approvedFound = true;
                        long dateInMillis = Long.parseLong(loggingInfo.getDate());
                        if (dateInMillis < timestamp) {
                            providerToServiceCountApprovedBeforeTimestamp.put(resourceOrganisation,
                                    providerToServiceCountApprovedBeforeTimestamp.getOrDefault(resourceOrganisation, 0) + 1);
                        }
                        break;
                    }
                } catch (Exception e) {
                    if (!providerOfServicesWithMalformedLoggingInfo.contains(resourceOrganisation)
                            && service.getStatus().equals("approved resource")) {
                        providerOfServicesWithMalformedLoggingInfo.add(resourceOrganisation);
                    }
                }
            }
            // for old registries with no or malformed status
            if (!approvedFound && !rejectedFound) {
                providerToServiceCountApprovedBeforeTimestamp.put(resourceOrganisation,
                        providerToServiceCountApprovedBeforeTimestamp.getOrDefault(resourceOrganisation, 0) + 1);
            }
        }

        // for old registries with no or malformed logging info
        for (String resourceOrganisation : providerOfServicesWithMalformedLoggingInfo) {
            providerToServiceCountApprovedBeforeTimestamp.put(resourceOrganisation,
                    providerToServiceCountApprovedBeforeTimestamp.getOrDefault(resourceOrganisation, 0) + 1);
        }

        listNumberOfServicesPerProviderCountryToCSV(providerIdToCountry, providerIdToName,
                providerToServiceCountApprovedBeforeTimestamp, response);
    }

    private void listNumberOfServicesPerProviderCountryToCSV(Map<String, String> providerIdToCountry,
                                                             Map<String, String> providerIdToName,
                                                             Map<String, Integer> providerToServiceCountApprovedBeforeTimestamp,
                                                             HttpServletResponse response) throws IOException {
        // Sort the entries by country
        List<Map.Entry<String, Integer>> sortedEntries = providerToServiceCountApprovedBeforeTimestamp.entrySet().stream()
                .sorted((entry1, entry2) -> {
                    String country1 = providerIdToCountry.get(entry1.getKey());
                    String country2 = providerIdToCountry.get(entry2.getKey());
                    return country1.compareTo(country2);
                })
                .toList();

        // Create CSV content
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Provider Name,Country,Service Count\n");
        for (Map.Entry<String, Integer> entry : sortedEntries) {
            String providerName = formatCSVField(providerIdToName.get(entry.getKey()));
            String country = formatCSVField(providerIdToCountry.get(entry.getKey()));
            String serviceCount = entry.getValue().toString();

            csvContent.append(providerName)
                    .append(',')
                    .append(country)
                    .append(',')
                    .append(serviceCount)
                    .append('\n');
        }

        // Set the response headers
        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"services_per_country.csv\"");

        // Write the CSV content to the response
        response.getWriter().write(csvContent.toString());
        response.getWriter().flush();
    }

    private static String formatCSVField(String field) {
        if (field == null) return "";
        if (field.contains("\"")) {
            field = field.replace("\"", "\"\"");
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            field = "\"" + field + "\"";
        }
        return field;
    }
}
