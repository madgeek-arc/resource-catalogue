package eu.openminted.registry.service;

import eu.openminted.registry.domain.Corpus;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/request/corpus")
public class CorpusController extends GenericRestController<Corpus>{

    final private CorpusService corpusService;

    @Autowired
    CorpusController(CorpusService service) {
        super(service);
        this.corpusService = service;
    }

    @RequestMapping(value = "upload", method = RequestMethod.POST)
    public ResponseEntity<String> uploadCorpus(@RequestParam("filename") String filename, @RequestParam("file") MultipartFile file) {

        try {
            return new ResponseEntity<>(corpusService.uploadCorpus(filename, file.getInputStream()), HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "download", method = RequestMethod.GET)
    @ResponseBody
    public void downloadCorpus(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String mimeType = "application/zip";
        String filename = request.getParameter("archiveId") + ".zip";
        response.setHeader("Content-Disposition", "attachment; filename=\""+ filename +"\"");
        response.setContentType(mimeType);
        IOUtils.copyLarge(corpusService.downloadCorpus(request.getParameter("archiveId")), response.getOutputStream());
    }
}