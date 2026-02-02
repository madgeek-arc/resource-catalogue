package gr.uoa.di.madgik.resourcecatalogue.controllers;

import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "user")
@Tag(name = "user")
public class UserController {

    @GetMapping(value = "info")
    public ResponseEntity<UserInfo> getUser(Authentication authentication) {
        return new ResponseEntity<>(UserInfo.of(authentication), HttpStatus.OK);
    }

}
