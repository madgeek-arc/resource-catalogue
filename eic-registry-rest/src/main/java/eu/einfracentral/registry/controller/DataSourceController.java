package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.DataSourceBundle;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.registry.service.DataSourceService;
import eu.einfracentral.registry.service.ProviderService;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.sql.DataSource;

@RestController
@RequestMapping({"dataSource"})
@Api(description = "Operations for DataSources")
public class DataSourceController {

    private static final Logger logger = LogManager.getLogger(DataSourceController.class);
    private final DataSourceService<DataSourceBundle, DataSourceBundle> dataSourceService;
    private final ProviderService<ProviderBundle, Authentication> providerService;
    private final DataSource commonDataSource;

    @Autowired
    DataSourceController(DataSourceService<DataSourceBundle, DataSourceBundle> service,
                         ProviderService<ProviderBundle, Authentication> provider,
                         DataSource commonDataSource) {
        this.dataSourceService = service;
        this.providerService = provider;
        this.commonDataSource = commonDataSource;
    }

    @ApiOperation(value = "Creates a new DataSource.")
    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public ResponseEntity<eu.einfracentral.domain.DataSource> addDataSource(@RequestBody eu.einfracentral.domain.DataSource dataSource, @ApiIgnore Authentication auth) {
        DataSourceBundle ret = this.dataSourceService.addDataSource(new DataSourceBundle(dataSource), auth);
        logger.info("User '{}' created a new DataSource with name '{}' and id '{}'", auth.getName(), dataSource.getName(), dataSource.getId());
        return new ResponseEntity<>(ret.getDataSource(), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Updates the DataSource assigned the given id with the given DataSource, keeping a version of revisions.")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    @PutMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<eu.einfracentral.domain.DataSource> updateService(@RequestBody eu.einfracentral.domain.DataSource dataSource,
                                                                            @RequestParam(required = false) String comment,
                                                                            @ApiIgnore Authentication auth) throws ResourceNotFoundException {
        DataSourceBundle ret = this.dataSourceService.updateDataSource(new DataSourceBundle(dataSource), comment, auth);
        logger.info("User '{}' updated DataSource with name '{}' and id '{}'", auth.getName(), dataSource.getName(), dataSource.getId());
        return new ResponseEntity<>(ret.getDataSource(), HttpStatus.OK);
    }

}
