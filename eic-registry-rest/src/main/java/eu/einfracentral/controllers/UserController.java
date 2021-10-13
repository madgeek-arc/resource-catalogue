package eu.einfracentral.controllers;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "user", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {
    private static final Logger logger = LogManager.getLogger(UserController.class);

    @GetMapping(value = "/info")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Map<String, Object>> login() {
        OIDCAuthenticationToken authentication = (OIDCAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        logger.debug("User authentication : " + authentication);
        Map<String,Object> user = new HashMap<>();
        user.put("sub",authentication.getSub());
        if(authentication.getUserInfo().getName() == null || authentication.getUserInfo().getName().equals(""))
            user.put("name",authentication.getUserInfo().getGivenName() + " " + authentication.getUserInfo().getFamilyName());
        else
            user.put("name",authentication.getUserInfo().getName());

        user.put("email",authentication.getUserInfo().getEmail());
        List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
        user.put("roles",roles);

        return new ResponseEntity<>(user, HttpStatus.OK);
    }
}
