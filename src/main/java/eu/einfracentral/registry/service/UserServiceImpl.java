package eu.einfracentral.registry.service;

import eu.einfracentral.domain.User;
import eu.einfracentral.exception.RESTException;
import eu.einfracentral.service.MailService;
import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.service.SearchService;
import io.jsonwebtoken.*;
import java.net.UnknownHostException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import javax.annotation.PostConstruct;
import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.beans.factory.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;

/**
 * Created by pgl on 07/08/17.
 */
@org.springframework.stereotype.Service("userService")
@Configurable
@PropertySource({"classpath:application.properties"})
public class UserServiceImpl<T> extends BaseGenericResourceCRUDServiceImpl<User> implements UserService {
    @Autowired
    private MailService mailService;
    @Value("${mail.activate.subject}")
    private String activateSubject;
    @Value("${mail.reset.subject}")
    private String resetSubject;
    @Value("${mail.activate.text}")
    private String activateText;
    @Value("${mail.reset.text}")
    private String resetText;
    @Value("${sec.user.iterations:1000}")
    private int currentServerIterationCount;
    @Value("${jwt.secret:}")
    private String secret;

    public UserServiceImpl() {
        super(User.class);
    }

    @PostConstruct
    private void postConstruct() {
        System.err.println(secret);
    }

    @Override
    public User activate(String id) {
        User ret = unsafeGet(id);
        if (ret.getJoinDate() == null) {
            ret.setJoinDate(new Date().toString());
            update(ret);
            //Rollback error exists up to 1.3.1-20170804.135357-7, other errors appear aftewards
        } else {
            throw new RESTException("User already activated", HttpStatus.CONFLICT);
        }
        return strip(ret);
    }

    @Override
    public User reset(User user) {
        User ret = null;
        if (user.getResetToken().equals(unsafeGet(user.getId()).getResetToken())) {
            ret = hashUser(user);
            update(ret);
        }
        return strip(ret);
    }

    @Override
    public User register(User user) {
        User ret = null;
        if (getUserByEmail(user.getEmail()) == null) {
            user.setId(UUID.randomUUID().toString());
            ret = hashUser(user);
            add(ret);
            mailService.sendMail(user.getEmail(), activateSubject, activateText + user.getId());
        } else {
            throw new RESTException("User already registered!", HttpStatus.CONFLICT);
        }
        return strip(ret); //Not using get(ret.getId()) here, because this line runs before the db is updated
    }

    @Override
    public User forgot(String email) {
        User ret = getUserByEmail(email);
        if (ret != null) {
            ret.setResetToken("Generate THIS!");
            update(ret);
            mailService.sendMail(ret.getEmail(), resetSubject, resetText + ret.getId() + "/" + ret.getResetToken());
        }
        return strip(ret);
    }

    @Override
    public String getToken(User credentials) {
        if (secret.length() == 0) {
            throw new RESTException("jwt.secret has not been set", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Date now = new Date();
        if (authenticate(credentials)) {
            return Jwts.builder()
                       .setSubject(credentials.getEmail())
                       .claim("roles", "user")
                       .setIssuedAt(now)
                       .setExpiration(new Date(now.getTime() + 86400000))
                       .signWith(SignatureAlgorithm.HS256, secret)
                       .compact();
        } else {
            throw new RESTException("Passwords do not match.", HttpStatus.FORBIDDEN);
        }
    }

    @Override
    public boolean authenticate(User credentials) {
        User actual = unsafeGet(getUserByEmail(credentials.getEmail()).getId());
        return Arrays.equals(hashPass(credentials.getPassword().toCharArray(),
                                      actual.getSalt(),
                                      actual.getIterationCount()), actual.getPassword().toCharArray());
    }

    @Override
    public User getUserByEmail(String email) {
        User ret = null;
        try {
            Resource foundResource = searchService.searchId(getResourceType(),
                                                            new SearchService.KeyValue("email", email));
            if (foundResource != null) {
                User foundUser = parserPool.serialize(foundResource, typeParameterClass).get();
                if (foundUser != null) {
                    ret = strip(foundUser);
                }
            }
        } catch (UnknownHostException | InterruptedException | ExecutionException e) {
            throw new RESTException(e, HttpStatus.NOT_FOUND);
        }
        return ret;
    }

    @Override
    public String getResourceType() {
        return "einfrauser";
    }

    private User hashUser(User user) {
        final Random r = new SecureRandom();
        byte[] salt = new byte[8];
        r.nextBytes(salt);
        user.setSalt(salt);
        user.setIterationCount(currentServerIterationCount);
        user.setPassword(new String(hashPass(user.getPassword().toCharArray(),
                                             user.getSalt(),
                                             user.getIterationCount())));
        return user;
    }

    private char[] hashPass(char[] pass, byte[] salt, int iterations) {
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            PBEKeySpec spec = new PBEKeySpec(pass, salt, iterations, 256);
            SecretKey key = skf.generateSecret(spec);
            return new String(Base64.getEncoder().encode(key.getEncoded())).toCharArray();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new Error(ex);
        }
    }

    private User unsafeGet(String id) {
        return super.get(id);
    }

    private User strip(User user) {
        user.setPassword("");
        user.setResetToken("");
        user.setSalt(new byte[0]);
        user.setIterationCount(0);
        return user;
    }

    @Override
    public User get(String id) {
        return strip(unsafeGet(id));
    }

    @Override
    public Browsing getAll(FacetFilter facetFilter) {
        return new Browsing(0, 0, 0, new ArrayList<User>(), new ArrayList<Facet>());
    }
}
