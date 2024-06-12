package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceInteroperabilityRecordService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Profile("crud")
@RestController
@RequestMapping(path = "resource-interoperability-records")
@Tag(name = "resource interoperability records")
public class ResourceInteroperabilityRecordCrudController extends ResourceCrudController<ResourceInteroperabilityRecordBundle> {

    private static final Logger logger = LogManager.getLogger(ResourceInteroperabilityRecordCrudController.class);

    private final ResourceInteroperabilityRecordService resourceInteroperabilityRecordService;

    public ResourceInteroperabilityRecordCrudController(ResourceInteroperabilityRecordService resourceInteroperabilityRecordService) {
        super(resourceInteroperabilityRecordService);
        this.resourceInteroperabilityRecordService = resourceInteroperabilityRecordService;
    }

    @PostMapping(path = "/bulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<ResourceInteroperabilityRecordBundle> bundles, @Parameter(hidden = true) Authentication auth) {
        resourceInteroperabilityRecordService.addBulk(bundles, auth);
    }
}
