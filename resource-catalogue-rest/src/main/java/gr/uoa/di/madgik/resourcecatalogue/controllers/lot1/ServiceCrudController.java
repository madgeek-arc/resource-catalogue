package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceBundleService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Profile("crud")
@RestController
@RequestMapping(path = "services")
@Tag(name = "services")
public class ServiceCrudController extends ResourceCrudController<ServiceBundle> {

    private static final Logger logger = LogManager.getLogger(ServiceCrudController.class.getName());
    private final ServiceBundleService<ServiceBundle> serviceBundleService;

    ServiceCrudController(ServiceBundleService<ServiceBundle> serviceBundleService) {
        super(serviceBundleService);
        this.serviceBundleService = serviceBundleService;
    }

    @PostMapping(path = "/bulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<ServiceBundle> bundles, @Parameter(hidden = true) Authentication auth) {
        serviceBundleService.addBulk(bundles, auth);
    }
}
