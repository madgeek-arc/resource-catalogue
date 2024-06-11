package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.annotations.Browse;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ResourceInteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.exception.ResourceException;
import gr.uoa.di.madgik.resourcecatalogue.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.InteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.ResourceInteroperabilityRecordService;
import gr.uoa.di.madgik.resourcecatalogue.service.SecurityService;
import gr.uoa.di.madgik.resourcecatalogue.utils.FacetFilterUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Profile("crud")
@RestController
@RequestMapping(path ="interoperability-records")
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