package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.AuditingInfo;
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

    private static final Logger logger = LogManager.getLogger(AuditingInfo.class);
    private AuditingInfoService<AuditingInfo, Authentication> auditingInfoService;


    @Autowired
    AuditingInfoController(AuditingInfoService<AuditingInfo, Authentication> auditingInfoService) {
        this.auditingInfoService = auditingInfoService;
    }

}
