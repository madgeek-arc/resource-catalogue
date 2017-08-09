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

    @CrossOrigin
    @RequestMapping(value = "register", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> register(@RequestBody User user) {
        user.setId("token");
        this.service.add(user, ParserService.ParserServiceTypes.JSON);

        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(user.getEmail());
        email.setSubject("[eInfraCentral] Activate your account");
        email.setText("Please visit http://einfracentral.eu/eic-registry/user/activate/" + user.getId());
        email.setFrom("test.espas@gmail.com");
        email.setReplyTo("test.espas@gmail.com");
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("smtp.gmail.com");
        sender.setPort(465);
        sender.setUsername("test.espas@gmail.com");
        sender.setPassword("s.a.g.a.p.w");
        Properties jmp = new Properties();
        jmp.setProperty("mail.transport.protocol", "ssl");
        jmp.setProperty("mail.smtp.auth", "true");
        jmp.setProperty("mail.smtp.starttls.enable", "true");
        sender.setJavaMailProperties(jmp);
        sender.send(email);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
