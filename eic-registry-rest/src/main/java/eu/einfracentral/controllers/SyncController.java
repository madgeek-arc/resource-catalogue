package eu.einfracentral.controllers;

import eu.einfracentral.domain.InfraService;
import eu.einfracentral.service.SynchronizerService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("sync")
public class SyncController {

    private static final Logger logger = LogManager.getLogger(SyncController.class);
    private final SynchronizerService<InfraService> serviceSync;

    @Autowired
    SyncController(SynchronizerService<InfraService> serviceSync) {
        this.serviceSync = serviceSync;
    }

    @PutMapping(path = "services")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<String> syncServices() {
        logger.info("Attempting to sync all Service resources");
        serviceSync.syncAll();
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
