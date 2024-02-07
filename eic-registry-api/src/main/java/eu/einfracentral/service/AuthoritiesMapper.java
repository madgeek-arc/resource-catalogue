package eu.einfracentral.service;

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
     * Update Authorities
     */
    void updateAuthorities();
}
