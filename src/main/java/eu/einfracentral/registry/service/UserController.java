package eu.einfracentral.registry.service;

import eu.einfracentral.domain.aai.User;
import eu.openminted.registry.core.service.ParserService;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.bind.annotation.*;

import java.util.Properties;

/**
 * Created by pgl on 07/08/17.
 */
@RestController
@RequestMapping("user")
public class UserController extends GenericRestController<User> {
    final private UserService userService;

    @Autowired
    UserController(UserService service) {
        super(service);
        this.userService = service;
    }

    @CrossOrigin
    @RequestMapping(path = "activate/{token}", method = RequestMethod.GET)
    public void activate(@PathVariable String token) {
        this.userService.activate(token);
    }

//    @CrossOrigin
//    @RequestMapping(value = "register", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
//    public ResponseEntity<String> registerJSON(@RequestBody User user) {
//        this.userService.register(user);
//        return new ResponseEntity<>(HttpStatus.OK);
//    }
//
//    @CrossOrigin
//    @RequestMapping(value = "register", method = RequestMethod.POST, consumes = MediaType.APPLICATION_XML_VALUE)
//    public ResponseEntity<String> registerXML(@RequestBody String body) {
//        this.userService.register(body);
//        return new ResponseEntity<>(HttpStatus.OK);
//    }
}
