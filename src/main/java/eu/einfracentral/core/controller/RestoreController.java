package eu.einfracentral.core.controller;

import eu.openminted.registry.core.service.RestoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Created by pgl on 08/01/18.
 */
@RestController
public class RestoreController {
    @Autowired
    RestoreService restoreService;

    @RequestMapping(path = "restore", method = RequestMethod.POST)
    public void restoreAll(@RequestParam("file") MultipartFile file) {
        restoreService.restoreDataFromZip(file);
    }
}
