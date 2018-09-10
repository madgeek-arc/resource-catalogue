//package eu.einfracentral.registry.controller;
//
//import eu.einfracentral.domain.User;
//import eu.einfracentral.registry.service.UserService;
//import eu.openminted.registry.core.domain.Browsing;
//import eu.openminted.registry.core.exception.ResourceNotFoundException;
//import io.swagger.annotations.ApiOperation;
//import java.util.Map;
//import javax.servlet.http.*;
//
//import org.apache.log4j.LogManager;
//import org.apache.log4j.Logger;
//import org.mitre.openid.connect.model.OIDCAuthenticationToken;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.*;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.util.WebUtils;
//
//@RestController
//@RequestMapping("user")
//public class UserController extends ResourceController<User, Authentication> {
//
//    final static private Logger logger = LogManager.getLogger(UserController.class);
//
//    @Autowired
//    UserController(UserService service) {
//        super(service);
//    }
//
//    @CrossOrigin
//    @RequestMapping(path = "activate/{token}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
//    public ResponseEntity<User> activate(@PathVariable String token) {
//        return new ResponseEntity<>(((UserService) service).activate(token), HttpStatus.OK);
//    }
//
//    @CrossOrigin
//    @RequestMapping(path = "reset", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
//    public ResponseEntity<User> reset(@RequestBody User user) {
//        return new ResponseEntity<>(((UserService) service).reset(user), HttpStatus.OK);
//    }
//
//    @CrossOrigin
//    @RequestMapping(path = "forgot", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
//    public ResponseEntity<User> forgot(@PathVariable String email) {
//        return new ResponseEntity<>(((UserService) service).forgot(email), HttpStatus.OK);
//    }
//
//    @CrossOrigin
//    @RequestMapping(path = "register", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
//    public ResponseEntity<User> register(@RequestBody User user) {
//        return new ResponseEntity<>(((UserService) service).register(user), HttpStatus.OK);
//    }
//
//    @CrossOrigin
//    @ApiOperation(value = "Issues the jwt")
//    @RequestMapping(path = "login", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
//    public ResponseEntity<User> login(@RequestBody User credentials, HttpServletResponse res) {
//        if (credentials.getEmail() == null || credentials.getPassword() == null) {
//            return new ResponseEntity<>(credentials, HttpStatus.UNPROCESSABLE_ENTITY);
//        }
//        if (!((UserService) service).authenticate(credentials)) {
//            return new ResponseEntity<>(credentials, HttpStatus.FORBIDDEN);
//        }
//        User ret = ((UserService) service).getUserByEmail(credentials.getEmail());
//        if (ret == null) {
//            return new ResponseEntity<>(credentials, HttpStatus.NOT_FOUND);
//        }
//        try {
//            String token = ((UserService) service).getToken(credentials);
//            Cookie cookie = new Cookie("jwt", token);
//            cookie.setHttpOnly(true);
//            cookie.setSecure(false);
//            res.addCookie(cookie);
//            return new ResponseEntity<>(ret, HttpStatus.OK);
//        } catch (Throwable e) {
//            return new ResponseEntity<>(credentials, HttpStatus.BAD_REQUEST);
//        }
//    }
//
////    @RequestMapping(path = "list", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
////    public ResponseEntity<Browsing<User>> getAll(@RequestParam Map<String, Object> allRequestParams, HttpServletRequest request) throws ResourceNotFoundException {
////        if (request.getRemoteAddr().equals("194.177.192.118")) {
////            return super.getAll(allRequestParams, WebUtils.getCookie(request, "jwt").getValue()); //TODO: Only allow verified admin user access to this
////        } else {
////            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
////        }
////    }
//
//    @ApiOperation(value = "Refresh user session")
//    @RequestMapping(path = "reLogin", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
//    @PreAuthorize("hasRole('ROLE_USER')")
//    public ResponseEntity<Object> reLogin() {
//        OIDCAuthenticationToken authOIDC = (OIDCAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
////        authOIDC.
////        ret.setAuthenticationSuccessHandler((httpServletRequest, response, authentication) -> {
////            Cookie sessionCookie = new Cookie("info", Base64.encode(authOIDC.getUserInfo().toJson().toString()).toString());
////            int expireSec = -1;
////            sessionCookie.setMaxAge(expireSec);
////            sessionCookie.setPath("/");
////            response.addCookie(sessionCookie);
//            logger.info(authOIDC.getUserInfo().toJson().toString());
//            logger.info(authOIDC.getAccessTokenValue());
//            logger.info(authOIDC.getCredentials());
//            logger.info(authOIDC.getSub());
////        }
//        return new ResponseEntity<>(authOIDC.getUserInfo().toJson(), HttpStatus.ACCEPTED);
//    }
//}
