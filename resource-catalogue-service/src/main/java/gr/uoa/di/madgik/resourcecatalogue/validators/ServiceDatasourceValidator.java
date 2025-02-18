package gr.uoa.di.madgik.resourcecatalogue.validators;

import gr.uoa.di.madgik.resourcecatalogue.domain.Service;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.utils.VocabularyValidationUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component("serviceValidator")
public class ServiceDatasourceValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return ServiceBundle.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Service service = ((ServiceBundle) target).getPayload();
        VocabularyValidationUtils.validateCategories(service.getCategories());
        VocabularyValidationUtils.validateScientificDomains(service.getScientificDomains());
    }
}
