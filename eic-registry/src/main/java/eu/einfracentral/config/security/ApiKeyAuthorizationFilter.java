package eu.einfracentral.config.security;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.mitre.openid.connect.client.OIDCAuthenticationProvider;
import org.mitre.openid.connect.client.service.ServerConfigurationService;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.mitre.openid.connect.model.PendingOIDCAuthenticationToken;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;

public class ApiKeyAuthorizationFilter extends GenericFilterBean {

    private static final Logger log = LogManager.getLogger(ApiKeyAuthorizationFilter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final ServerConfigurationService serverConfigurationService;

    private final AuthenticationProvider authenticationProvider;

    public ApiKeyAuthorizationFilter(ServerConfigurationService serverConfigurationService,
                                     OIDCAuthenticationProvider openIdConnectAuthenticationProvider) {
//        super("/**");
//        setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/**"));
        this.serverConfigurationService = serverConfigurationService;
        this.authenticationProvider = openIdConnectAuthenticationProvider;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res,
                         FilterChain chain) throws IOException, ServletException {
        log.debug("Attempt Authentication");
        HttpServletRequest request = (HttpServletRequest) req;
        String jwt = resolveToken(request);
        PendingOIDCAuthenticationToken token;
        try {
            if (jwt == null) {
                throw new NullPointerException("jwt is null");
            }
            JWT idToken = JWTParser.parse(jwt);
            String issuer = idToken.getJWTClaimsSet().getIssuer();
            String subject = idToken.getJWTClaimsSet().getSubject();
            String accessToken = idToken.getParsedString();
            ServerConfiguration config = serverConfigurationService.getServerConfiguration(issuer);
            token = new PendingOIDCAuthenticationToken(subject, issuer, config, idToken, accessToken, null);
            Authentication auth = this.authenticationProvider.authenticate(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(req, res);
        } catch (RuntimeException | ParseException e) {
            log.error(e);
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ObjectMapper mapper = new ObjectMapper();
            UnauthorizedUserException exception = new UnauthorizedUserException(e.getMessage(), e);
            res.getWriter().append(mapper.writeValueAsString(exception));
            ((HttpServletResponse) res).setStatus(401);
        }

    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken)) {
            if (bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
            return bearerToken;
        }
        return null;
    }
}
