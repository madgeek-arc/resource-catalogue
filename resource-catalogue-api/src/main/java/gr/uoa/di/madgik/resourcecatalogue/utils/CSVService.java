package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;

import javax.servlet.http.HttpServletResponse;
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
     * @param response  HttpServletResponse
     */
    void computeApprovedServicesBeforeTimestampAndGenerateCSV(long timestamp,
                                                              List<ProviderBundle> providers,
                                                              List<ServiceBundle> services,
                                                              HttpServletResponse response) throws IOException;
}
