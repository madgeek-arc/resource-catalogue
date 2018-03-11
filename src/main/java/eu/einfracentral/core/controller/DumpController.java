package eu.einfracentral.core.controller;

import eu.openminted.registry.core.service.DumpService;
import java.io.*;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * Created by pgl on 08/01/18.
 */
@RestController
public class DumpController {
    @Autowired
    DumpService dumpService;

    @RequestMapping(path = "dump", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void dumpAll(@RequestParam(value = "types", required = false, defaultValue = "") String[] types, HttpServletResponse response) {
        File dump = dumpService.bringAll(true, true, types);
        response.setContentLength((int) dump.length());
        response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", dump.getName()));
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(dump));
             OutputStream out = response.getOutputStream();
             BufferedOutputStream bufout = new BufferedOutputStream(out)) {
            byte[] buf = new byte[8192];
            for (int bytes = in.read(buf); bytes != -1; bytes = in.read(buf)) {
                bufout.write(buf, 0, bytes);
            }
            bufout.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //delete dump directory on end?
        }
    }
}
