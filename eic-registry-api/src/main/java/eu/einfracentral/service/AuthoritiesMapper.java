package eu.einfracentral.service;

public interface AuthoritiesMapper {

    boolean isAdmin(String email);

    boolean isEPOT(String email);

    void updateAuthorities();
}
