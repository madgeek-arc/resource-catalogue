package eu.einfracentral.registry.service;

import eu.einfracentral.domain.aai.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

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
    public ResponseEntity<User> activate(@PathVariable String token) {
        return new ResponseEntity<>(this.userService.activate(token), HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(value = "register", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> registerJSON(@RequestBody User user) {
        this.userService.register(user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(value = "login", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<User> login(@RequestBody User credentials, HttpServletResponse res) {
        if (credentials.getUsername() == null || credentials.getPassword() == null)
            return new ResponseEntity<>(credentials, HttpStatus.UNPROCESSABLE_ENTITY);
        User ret = null;
        //User user =  this.getUserByUsername();
        //User user = get("pgl_user_id");
        ret = new User();
        ret.setUsername("pgl");
        ret.setPassword("my actual password irl");
        if (ret == null) return new ResponseEntity<>(credentials, HttpStatus.NOT_FOUND);
        if (!credentials.getPassword().equals(ret.getPassword()))
            return new ResponseEntity<>(credentials, HttpStatus.FORBIDDEN);
        String token = this.userService.getToken(ret);
        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        res.addCookie(cookie);
        ret.setPassword("");
        return new ResponseEntity<User>(ret, HttpStatus.OK);
    }

//    }
}
