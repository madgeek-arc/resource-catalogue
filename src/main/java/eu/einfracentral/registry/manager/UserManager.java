package eu.einfracentral.registry.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.einfracentral.config.ApplicationConfig;
import eu.einfracentral.domain.User;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.UserService;
import eu.einfracentral.service.MailService;
import eu.openminted.registry.core.domain.*;
import io.jsonwebtoken.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Created by pgl on 07/08/17.
 */
@Service("userService")
@Configurable
public class UserManager extends ResourceManager<User> implements UserService {
    @Autowired
    private MailService mailService;
    @Autowired
    private ApplicationConfig config;

    public UserManager() {
        super(User.class);
    }

    @Override
    public User activate(String id) {
        User ret = unsafeGet(id);
        if (ret.getJoinDate() == null) {
            ret.setJoinDate(new Date().toString());
            update(ret);
            //Rollback error exists up to 1.3.1-20170804.135357-7, other errors appear aftewards
        } else {
            throw new ResourceException("User already activated", HttpStatus.CONFLICT);
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
        if (where("email", user.getEmail(), false) == null) {
            user.setId(UUID.randomUUID().toString());
            ret = hashUser(user);
            add(ret);
            mailService.sendMail(user.getEmail(), config.getActivateSubject(), config.getActivateText() + user.getId());
        } else {
            throw new ResourceException("User already registered!", HttpStatus.CONFLICT);
        }
        return strip(ret); //Not using get(ret.getId()) here, because this line runs before the db is updated
    }

    @Override
    public User forgot(String email) {
        User ret = getUserByEmail(email);
        if (ret != null) {
            ret.setResetToken("Generate THIS!");
            update(ret);
            mailService.sendMail(ret.getEmail(), config.getResetSubject(), config.getResetText() + ret.getId() + "/" + ret.getResetToken());
        }
        return strip(ret);
    }

    @Override
    public String getToken(User credentials) {
        if (config.getSecret().length() == 0) {
            throw new ResourceException("jwt.secret has not been set", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Date now = new Date();
        String payload;
        if (authenticate(credentials)) {
            try {
                payload = new ObjectMapper().writeValueAsString(credentials);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw new ResourceException("Could not stringify user.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return Jwts.builder()
                       .setSubject(credentials.getEmail())
                       .setPayload(payload)
                       .setIssuedAt(now)
                       .setExpiration(new Date(now.getTime() + 86400000))
                       .signWith(SignatureAlgorithm.HS256, config.getSecret())
                       .compact();
        } else {
            throw new ResourceException("Passwords do not match.", HttpStatus.FORBIDDEN);
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
        return strip(deserialize(where("email", email, true)));
    }

    @Override
    public User addFavourite(String userID, String serviceID) {
        return get(userID); //TODO: implement this
    }

    @Override
    public User get(String id) {
        return strip(unsafeGet(id));
    }

    @Override
    public Browsing<User> getAll(FacetFilter facetFilter) {
        return new Browsing<>(0, 0, 0, new ArrayList<User>(), new ArrayList<>());
    }

    private User hashUser(User user) {
        final Random r = new SecureRandom();
        byte[] salt = new byte[8];
        r.nextBytes(salt);
        user.setSalt(salt);
        user.setIterationCount(config.getIterations());
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
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
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
    public String getResourceType() {
        return "einfrauser";
    }
}
