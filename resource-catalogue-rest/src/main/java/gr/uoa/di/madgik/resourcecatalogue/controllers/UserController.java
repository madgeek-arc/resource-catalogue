package gr.uoa.di.madgik.resourcecatalogue.controllers;

import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "user")
public class UserController {

    public record UserInfo(String sub, String email, String name, String surname, List<String> roles) {}

    @GetMapping(value = "info")
    public ResponseEntity<UserInfo> getUser(Authentication authentication) {
        User user = User.of(authentication);
        List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        return new ResponseEntity<>(new UserInfo(user.getId(), user.getEmail(), user.getName(), user.getSurname(), roles), HttpStatus.OK);
    }

}
