/**
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

package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;

import java.io.IOException;
import java.util.List;

public interface CSVService {
    /**
     * Create a CSV from a list of Providers
     *
     * @param list Provider list
     * @return {@link String}
     */
    String listProvidersToCSV(List<ProviderBundle> list);

    /**
     * Create a CSV from a list of Services
     *
     * @param list Service list
     * @return {@link String}
     */
    String listServicesToCSV(List<ServiceBundle> list);

    /**
     * Create a CSV from a list of Vocabularies
     *
     * @param list Vocabularies list
     * @return {@link String}
     */
    String listVocabulariesToCSV(List<Vocabulary> list);

    /**
     * Create a CSV from a list of Vocabularies
     *
     * @param date Date (yyyy-MM-dd)
     * @return {@link long}
     */
    long generateTimestampFromDate(String date);

    /**
     * Create a CSV from
     *
     * @param timestamp Date in Timestamp
     * @param providers List of Providers
     * @param services  List of Services
     * @return The CSV as String.
     */
    String computeApprovedServicesBeforeTimestampAndGenerateCSV(long timestamp,
                                                              List<ProviderBundle> providers,
                                                              List<ServiceBundle> services) throws IOException;
}
