/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.manager;

import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.utils.CSVService;

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

    public String computeApprovedServicesBeforeTimestampAndGenerateCSV(long timestamp,
                                                                     List<ProviderBundle> providers,
                                                                     List<ServiceBundle> services) {
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

        return listNumberOfServicesPerProviderCountryToCSV(providerIdToCountry, providerIdToName,
                providerToServiceCountApprovedBeforeTimestamp);
    }

    private String listNumberOfServicesPerProviderCountryToCSV(Map<String, String> providerIdToCountry,
                                                             Map<String, String> providerIdToName,
                                                             Map<String, Integer> providerToServiceCountApprovedBeforeTimestamp) {
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

        return csvContent.toString();
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
