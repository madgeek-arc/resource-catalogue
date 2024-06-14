package gr.uoa.di.madgik.resourcecatalogue.service;

import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

public interface AuthoritiesMapper {

    /**
     * Return True if User is Admin
     *
     * @param email User's email
     * @return True/False
     */
    boolean isAdmin(String email);

    /**
     * Return True if User is EPOT
     *
     * @param email User's email
     * @return True/False
     */
    boolean isEPOT(String email);

    /**
     * Returns user's authorities.
     *
     * @param email User's email
     * @return the authorities of the user
     */
    Set<GrantedAuthority> getAuthorities(String email);

    /**
     * Update Authorities
     */
    void updateAuthorities();
}
