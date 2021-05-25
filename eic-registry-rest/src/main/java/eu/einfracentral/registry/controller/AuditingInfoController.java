package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.BundleStatus;
import eu.einfracentral.registry.service.AuditingInfoService;
import io.swagger.annotations.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auditingInfo")
@Api(value = "Get information about a Auditing")
public class AuditingInfoController {

    private static final Logger logger = LogManager.getLogger(BundleStatus.class);
    private AuditingInfoService<BundleStatus, Authentication> auditingInfoService;


    @Autowired
    AuditingInfoController(AuditingInfoService<BundleStatus, Authentication> auditingInfoService) {
        this.auditingInfoService = auditingInfoService;
    }

}
