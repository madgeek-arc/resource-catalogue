package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
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
@RequestMapping(path = "interoperability-records")
@Tag(name = "interoperability records")
public class InteroperabilityRecordCrudController extends ResourceCrudController<InteroperabilityRecordBundle> {

    private static final Logger logger = LogManager.getLogger(InteroperabilityRecordCrudController.class);
    private final InteroperabilityRecordService interoperabilityRecordService;

    public InteroperabilityRecordCrudController(InteroperabilityRecordService interoperabilityRecordService) {
        super(interoperabilityRecordService);
        this.interoperabilityRecordService = interoperabilityRecordService;
    }

    @PostMapping(path = "/bulk")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void addBulk(@RequestBody List<InteroperabilityRecordBundle> bundles, @Parameter(hidden = true) Authentication auth) {
        interoperabilityRecordService.addBulk(bundles, auth);
    }
}