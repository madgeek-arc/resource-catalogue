package eu.einfracentral.registry.controller;

import eu.einfracentral.domain.User;
import eu.einfracentral.registry.service.UserService;
import eu.openminted.registry.core.domain.Browsing;
import io.swagger.annotations.ApiOperation;
import java.util.Map;
import javax.servlet.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.WebUtils;

/**
 * Created by pgl on 07/08/17.
 */
@RestController
@RequestMapping("user")
public class UserController extends ResourceController<User> {
    @Autowired
    UserController(UserService service) {
        super(service);
    }

    @CrossOrigin
    @RequestMapping(path = "activate/{token}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<User> activate(@PathVariable String token) {
        return new ResponseEntity<>(((UserService) service).activate(token), HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(path = "reset", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<User> reset(@RequestBody User user) {
        return new ResponseEntity<>(((UserService) service).reset(user), HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(path = "forgot", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<User> forgot(@PathVariable String email) {
        return new ResponseEntity<>(((UserService) service).forgot(email), HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(path = "register", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<User> register(@RequestBody User user) {
        return new ResponseEntity<>(((UserService) service).register(user), HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(path = "addFavourite", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<User> addFavourite(@RequestParam String userID, @RequestParam String serviceID) {
        return new ResponseEntity<>(((UserService) service).addFavourite(userID, serviceID), HttpStatus.OK);
    }

    @CrossOrigin
    @ApiOperation(value = "Issues the jwt")
    @RequestMapping(path = "login", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<User> login(@RequestBody User credentials, HttpServletResponse res) {
        if (credentials.getEmail() == null || credentials.getPassword() == null) {
            return new ResponseEntity<>(credentials, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!((UserService) service).authenticate(credentials)) {
            return new ResponseEntity<>(credentials, HttpStatus.FORBIDDEN);
        }
        User ret = ((UserService) service).getUserByEmail(credentials.getEmail());
        if (ret == null) {
            return new ResponseEntity<>(credentials, HttpStatus.NOT_FOUND);
        }
        try {
            String token = ((UserService) service).getToken(credentials);
            Cookie cookie = new Cookie("jwt", token);
            cookie.setHttpOnly(true);
            cookie.setSecure(false);
            res.addCookie(cookie);
            return new ResponseEntity<>(ret, HttpStatus.OK);
        } catch (Throwable e) {
            return new ResponseEntity<>(credentials, HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "list", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<Browsing<User>> getAll(@RequestParam Map<String, Object> allRequestParams, HttpServletRequest request) {
        if (request.getRemoteAddr().equals("194.177.192.118")) {
            return super.getAll(allRequestParams, WebUtils.getCookie(request, "jwt").getValue()); //TODO: Only allow verified admin user access to this
        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }
}
