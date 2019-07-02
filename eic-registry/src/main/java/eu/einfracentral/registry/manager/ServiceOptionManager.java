package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.ServiceOption;
import eu.einfracentral.exception.ValidationException;
import eu.einfracentral.registry.service.ServiceOptionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ServiceOptionManager extends ResourceManager<ServiceOption> implements ServiceOptionService<ServiceOption, Authentication> {

    private static final Logger logger = LogManager.getLogger(ProviderManager.class);

    public ServiceOptionManager() {
        super(ServiceOption.class);
    }

    @Override
    public String getResourceType() {
        return "service_option";
    }

    @Override
    public ServiceOption add(ServiceOption serviceOption, Authentication auth){
        validate(serviceOption);
        super.add(serviceOption, auth);
        logger.info("Adding ServiceOption " +serviceOption);
        return serviceOption;
    }

    @Override
    public ServiceOption update(ServiceOption serviceOption, Authentication auth){
        validate(serviceOption);
        super.update(serviceOption, auth);
        logger.info("Updating ServiceOption " +serviceOption);
        return serviceOption;
    }

    public ServiceOption get(String id, Authentication auth) {
        return null;
    }

    @Override
    public void delete(ServiceOption serviceOption) {
        super.delete(serviceOption);
        logger.info("Deleting ServiceOption " + serviceOption);
    }

    @Override
    public ServiceOption validate(ServiceOption serviceOption) {

        // Validates ServiceOption's ID
        if (serviceOption.getId() == null || serviceOption.getId().equals("")) {
            throw new ValidationException("field 'id' is mandatory");
        }

        // Validates ServiceOption's name
        if (serviceOption.getName() == null || serviceOption.getName().equals("")) {
            throw new ValidationException("field 'name' is mandatory");
        }
        if (serviceOption.getName().length() > 80) {
            throw new ValidationException("max length for 'name' is 80 chars");
        }

        // Validates ServiceOption's url
        if (serviceOption.getUrl() == null || serviceOption.getUrl().toString().equals("")) {
            throw new ValidationException("field 'url' is mandatory");
        }

        // Validates ServiceOption's description
        if (serviceOption.getDescription() == null || serviceOption.getDescription().equals("")) {
            throw new ValidationException("field 'description' is mandatory");
        }
        if (serviceOption.getDescription().length() > 1000) {
            throw new ValidationException("max length for 'description' is 1000 chars");
        }

        return serviceOption;
    }

}
