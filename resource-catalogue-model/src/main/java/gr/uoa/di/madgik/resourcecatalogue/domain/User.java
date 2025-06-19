/**
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Objects;

@XmlType
@XmlRootElement
public class User implements Identifiable {

    private static final Logger logger = LoggerFactory.getLogger(User.class);

    @XmlElement
    private String id;

    @XmlElement
    @FieldValidation
    private String email;

    @XmlElement
    @FieldValidation
    private String name;

    @XmlElement
    @FieldValidation
    private String surname;

    public User() {
    }

    public User(String id, String email, String name, String surname) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.surname = surname;
    }

    public static User of(Authentication auth) {
        logger.trace("Creating User from Authentication\n{}", auth);
        User user = new User();
        if (auth == null) {
            throw new InsufficientAuthenticationException("You are not authenticated, please log in.");
        } else if (auth.getPrincipal() instanceof OidcUser principal) {
            user.id = principal.getSubject();
            if (user.id == null) {
                user.id = "";
            }
            user.email = principal.getEmail();
            if (user.email == null) {
                user.email = "";
            }
            user.name = principal.getGivenName();
            user.surname = principal.getFamilyName();
        } else if (auth instanceof JwtAuthenticationToken) {
            Jwt principal = (Jwt) auth.getPrincipal();
            user.id = principal.getClaimAsString("sub");
            user.email = principal.getClaimAsString("email");
            user.name = principal.getClaimAsString("given_name");
            user.surname = principal.getClaimAsString("family_name");
        } else if (auth instanceof OAuth2AuthenticationToken) {
            OAuth2User principal = ((OAuth2AuthenticationToken) auth).getPrincipal();
            user.id = principal.getAttribute("subject");
            user.email = principal.getAttribute("email");
            user.name = principal.getAttribute("given_name");
            user.surname = principal.getAttribute("family_name");
        } else if (auth instanceof AdminAuthentication) {
            logger.trace("internal admin access");
        } else if (auth.isAuthenticated()) {
            logger.warn("Authenticated User has missing information: {}", auth);
            return null;
        } else {
            throw new InsufficientAuthenticationException("Could not create user. Insufficient user authentication");
        }
        logger.debug("User from Authentication: {}", user);
        return user;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                '}';
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.toLowerCase() : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    @JsonIgnore
    public String getFullName() {
        return String.format("%s %s", this.name, this.surname);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(email, user.email) && Objects.equals(name, user.name) && Objects.equals(surname, user.surname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, name, surname);
    }
}
