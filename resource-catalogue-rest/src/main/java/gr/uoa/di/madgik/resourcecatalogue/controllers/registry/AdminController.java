package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.resourcecatalogue.domain.NewBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.TestService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Hidden
@Profile("beyond")
@RestController
@RequestMapping({"admin"})
@Tag(name = "admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final TestService<NewBundle> testService;

    public AdminController(TestService<NewBundle> testService) {
        this.testService = testService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<NewBundle> add(@RequestBody NewBundle bundle,
                                         @Parameter(hidden = true) Authentication auth) {
        NewBundle ret = testService.add(bundle, auth);
        logger.info("Added Bundle with id '{}' on the Catalogue '{}' and published '{}'",
                bundle.getId(), bundle.getCatalogueId(), bundle.getMetadata().isPublished());
        return new ResponseEntity<>(ret, HttpStatus.CREATED);
    }

    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<NewBundle> update(@RequestBody NewBundle bundle,
                                            @RequestParam(required = false) String comment,
                                            @Parameter(hidden = true) Authentication auth) {
        NewBundle ret = testService.update(bundle, comment, auth);
        logger.info("Updated Bundle with id '{}' of the Catalogue '{}'", bundle.getId(), bundle.getCatalogueId());
        return new ResponseEntity<>(ret, HttpStatus.OK);
    }

    @DeleteMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void delete(@RequestBody NewBundle bundle) {
        testService.delete(bundle);
        logger.info("Deleted Bundle with id '{}' of the Catalogue '{}'", bundle.getId(), bundle.getCatalogueId());
    }
}
