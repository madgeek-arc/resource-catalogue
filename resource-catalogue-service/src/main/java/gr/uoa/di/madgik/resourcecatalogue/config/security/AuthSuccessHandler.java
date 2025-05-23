/*
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

package gr.uoa.di.madgik.resourcecatalogue.config.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.resourcecatalogue.config.properties.CatalogueProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AuthSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(AuthSuccessHandler.class);

    private final CatalogueProperties catalogueProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthSuccessHandler(CatalogueProperties catalogueProperties) {
        this.catalogueProperties = catalogueProperties;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {
        AuthenticationSuccessHandler.super.onAuthenticationSuccess(request, response, chain, authentication);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        int expireSec = createCookieMaxAge(authentication);

        String userInfo = createUserInfoJson(authentication);
        Cookie cookie = new Cookie("info", new String(Base64.getEncoder().encode(userInfo.getBytes())));
        cookie.setMaxAge(expireSec);
        cookie.setPath("/");
//        cookie.setSecure(true);

        Cookie token = new Cookie("AccessToken", ((OidcUser) authentication.getPrincipal()).getIdToken().getTokenValue());
        token.setPath("/");
        response.addCookie(token);

        response.addCookie(cookie);
        response.sendRedirect(catalogueProperties.getLoginRedirect());
    }

    private String createUserInfoJson(Authentication authentication) throws JsonProcessingException {
        OidcUser user = (OidcUser) authentication.getPrincipal();
        List<String> roles = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        logger.info("Authentication: {}", authentication);
        logger.info("UserInfo: {}\nAuthorities: {}", user.getClaims(), roles);

        Map<String, Object> info = new HashMap<>();
        info.put("sub", user.getSubject());
        info.put("email", user.getEmail());
        info.put("given_name", user.getGivenName());
        info.put("family_name", user.getFamilyName());
        info.put("roles", roles);
        info.put("expireSec", createCookieMaxAge(authentication));

        return objectMapper.writeValueAsString(info);
    }

    private int createCookieMaxAge(Authentication authentication) {
        Integer age = getExp(authentication);
        return age != null ? age : 3600;
    }

    private Integer getExp(Authentication authentication) {
        OidcUser user = ((OidcUser) authentication.getPrincipal());
        if (user.getAttribute("exp") instanceof Instant) {
            Instant exp = user.getAttribute("exp");
            int age = (int) (exp.getEpochSecond() - (new Date().getTime() / 1000));
            return age;
        }
        return null;
    }
}
