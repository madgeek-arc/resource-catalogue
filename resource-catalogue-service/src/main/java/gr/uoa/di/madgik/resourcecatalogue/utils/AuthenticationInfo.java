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

package gr.uoa.di.madgik.resourcecatalogue.utils;

import gr.uoa.di.madgik.resourcecatalogue.exception.OidcAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.HashMap;
import java.util.Map;

public class AuthenticationInfo {

    private AuthenticationInfo() {
    }

    public static String getSub(Authentication auth) {
        return getClaims(auth).getOrDefault("sub", "null").toString();
    }

    public static String getEmail(Authentication auth) {
        return getClaims(auth).getOrDefault("email", "null").toString();
    }

    public static String getFullName(Authentication auth) {
        return getClaims(auth).getOrDefault("name", "null").toString();
    }

    public static String getGivenName(Authentication auth) {
        return getClaims(auth).getOrDefault("given_name", "null").toString();
    }

    public static String getFamilyName(Authentication auth) {
        return getClaims(auth).getOrDefault("family_name", "null").toString();
    }

    private static Map<String, Object> getClaims(Authentication auth) {
        if (auth == null) {
            return new HashMap<>();
        } else if (auth.getPrincipal() instanceof OidcUser principal) {
            return principal.getClaims();
        } else if (auth.getPrincipal() instanceof Jwt) {
            return ((Jwt) auth.getPrincipal()).getClaims();
        } else {
            throw new OidcAuthenticationException("Could not retrieve user details.");
        }
    }
}
