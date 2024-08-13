package gr.uoa.di.madgik.resourcecatalogue.validators;

import gr.uoa.di.madgik.resourcecatalogue.domain.Service;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.exception.ValidationException;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import gr.uoa.di.madgik.resourcecatalogue.utils.VocabularyValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.List;

@Component("serviceValidator")
public class ServiceDatasourceValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(ServiceDatasourceValidator.class);
    private final ServiceBundleService serviceBundleService;
    private final String catalogueName;

    @Autowired
    public ServiceDatasourceValidator(@Value("${catalogue.id}") String catalogueName,
                                      @Lazy ServiceBundleService serviceBundleService) {
        this.catalogueName = catalogueName;
        this.serviceBundleService = serviceBundleService;
    }


    @Override
    public boolean supports(Class<?> clazz) {
        return ServiceBundle.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Service service = ((ServiceBundle) target).getPayload();
        //TODO: Probably not needed -> related/required field validation was added on FieldValidator
//        if (service.getRelatedResources() != null && !service.getRelatedResources().isEmpty()){
//            validateRelatedResources(service, errors);
//        }
//        if (service.getRequiredResources() != null && !service.getRequiredResources().isEmpty()){
//            validateRequiredResources(service, errors);
//        }
        VocabularyValidationUtils.validateCategories(service.getCategories());
        VocabularyValidationUtils.validateScientificDomains(service.getScientificDomains());
    }

    private void validateRequiredResources(Service service, Errors errors) {
        validateServiceResources(service.getRelatedResources(), service.getCatalogueId(), "relatedResources", errors);
    }

    private void validateRelatedResources(Service service, Errors errors) {
        validateServiceResources(service.getRequiredResources(), service.getCatalogueId(), "requiredResources", errors);
    }


    private void validateServiceResources(List<String> resources, String catalogue, String field, Errors errors) {
        for (String resource : resources) {
            try {
                serviceBundleService.get(resource, catalogue);
            } catch (Exception e) {
                try {
                    serviceBundleService.get(resource, catalogueName);
                } catch (Exception ex) {
//                    errors.rejectValue("relatedResources", field + ".invalid", String.format("Could not find Service with id [%s] in Catalogue [%s]", resource, catalogue));
                    throw new ValidationException(String.format("Could not find Service with id [%s] in Catalogue [%s]", resource, catalogue));
                }
            }
        }
    }
}
