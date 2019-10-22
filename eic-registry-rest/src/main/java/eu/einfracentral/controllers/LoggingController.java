package eu.einfracentral.controllers;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("logging")
public class LoggingController {

    private static final Logger logger = LogManager.getLogger(LoggingController.class);

    @PostMapping("root")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity setRootLogLevel(@RequestParam Level standardLevel) {
        logger.info("Changing Root Level Logging to '{}'", standardLevel);
        Configurator.setRootLevel(standardLevel);
        return new ResponseEntity(HttpStatus.OK);
    }

    @PostMapping("package/{path}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity setPackageLogLevel(@PathVariable("path") String path, @RequestParam Level standardLevel) {
        logger.info("Changing '{}' Logger Level to '{}'", path, standardLevel);
        Configurator.setLevel(path, standardLevel);
        return new ResponseEntity(HttpStatus.OK);
    }

}
