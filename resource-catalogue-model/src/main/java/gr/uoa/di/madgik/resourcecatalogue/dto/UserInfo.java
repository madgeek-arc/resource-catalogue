package gr.uoa.di.madgik.resourcecatalogue.dto;

import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Objects;

public record UserInfo(String sub, String email, String name, String surname, List<String> roles) {

    public static UserInfo of(Authentication authentication) {
        User user = Objects.requireNonNull(User.of(authentication));
        return new UserInfo(
                user.getId(),
                user.getEmail().toLowerCase(),
                user.getName(),
                user.getSurname(),
                authentication.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .sorted()
                        .toList()
        );
    }

    public String fullName() {
        return "%s %s".formatted(name(), surname());
    }
}
