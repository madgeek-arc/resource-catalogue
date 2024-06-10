package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.resourcecatalogue.controllers.registry.ResourceController;
import gr.uoa.di.madgik.resourcecatalogue.domain.ProviderBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ProviderService;
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
@RequestMapping("providers")
@Tag(name = "providers")
public class ProviderCrudController extends ResourceController<ProviderBundle> {

    private static final Logger logger = LogManager.getLogger(ProviderCrudController.class);
    private final ProviderService providerService;

    ProviderCrudController(ProviderService providerService) {
        super(providerService);
        this.providerService = providerService;
    }

    @PostMapping(path = "/bulk", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<ProviderBundle> providerList, @Parameter(hidden = true) Authentication auth) {
        providerService.addBulk(providerList, auth);
    }

}
