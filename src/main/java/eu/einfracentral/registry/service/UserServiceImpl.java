package eu.einfracentral.registry.service;

import eu.einfracentral.domain.aai.User;
import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by pgl on 07/08/17.
 */
@org.springframework.stereotype.Service("userService")
public class UserServiceImpl<T> extends BaseGenericResourceCRUDServiceImpl<User> implements UserService/*, EnvironmentAware */ {

    @Autowired
    private Environment env;

    @Autowired
    private MailService mailService;

    public UserServiceImpl() {
        super(User.class);
    }

    @Override
    public String getResourceType() {
        return "einfrauser";
    }

    @Override
    public User activate(String id) {
        User ret = reveal(get(id));
        if (ret.getJoinDate() == null) {
            ret.setJoinDate(new Date().toString());
            update(ret);
            //Rollback error exists up to 1.3.1-20170804.135357-7, other errors appear aftewards
        }
        return ret;
    }

    @Override
    public User reset(User user) {
        User ret = null;
        if (user.getResetToken() == reveal(get(user.getId())).getResetToken()) {
            ret = hashUser(user);
            update(ret);
        }
        return ret;
    }

    @Override
    public User forgot(String email) {
        User ret = getUserByEmail(email);
        if (ret != null) {
            ret.setResetToken("Generate THIS!");
            update(ret);
            mailService.sendMail(ret.getEmail(), mailService.jmp.getProperty("smtp.activate.subject"),
                    mailService.jmp.getProperty("smtp.activate.text") + ret.getId() + "/" + ret.getResetToken());

        }
        return ret;
    }

    @Override
    public Browsing getAll(FacetFilter facetFilter) {
        return new Browsing(0, 0, 0, new ArrayList<Order>(), new ArrayList<Facet>());
    }

    @Override
    public User register(User user) {
        User ret = null;
        if (getUserByEmail(user.getEmail()) == null) {
            user.setId(UUID.randomUUID().toString());
            ret = hashUser(user);
            add(ret, ParserService.ParserServiceTypes.JSON);
            ret.setPassword("");
            mailService.sendMail(user.getEmail(), mailService.jmp.getProperty("mail.activate.subject"),
                    mailService.jmp.getProperty("mail.activate.text") + user.getId());
        }
        return ret; //Not using get(ret.getId()) here, because this line runs before the db is updated
    }

    private User hashUser(User user) {
        final Random r = new SecureRandom();
        byte[] salt = new byte[8];
        r.nextBytes(salt);
        user.setSalt(salt);
        user.setIterationCount(20000);
        user.setPassword(new String(hashPass(user.getPassword().toCharArray(), user.getSalt(), user.getIterationCount())));
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

    @Override
    public boolean authenticate(User credentials) {
        User actual = reveal(getUserByEmail(credentials.getEmail()));
        return hashPass(credentials.getPassword().toCharArray(), actual.getSalt(), actual.getIterationCount()).equals(actual.getPassword().toCharArray());
    }

    @Override
    public User getUserByEmail(String email) {
        Resource foundResource = null;
        User foundUser = null;
        User ret = null;
        try {
            foundResource = searchService.searchId(getResourceType(), new SearchService.KeyValue("email", email));
            if (foundResource != null) {
                foundUser = parserPool.serialize(foundResource, typeParameterClass).get();
                if (foundUser != null) {
                    ret = get(foundUser.getId());
                }
            }
        } catch (UnknownHostException | InterruptedException | ExecutionException e) {
            ret = null;
        }
        return ret;
    }

    @Override
    public String getToken(User credentials) {
        String secret = env.getProperty("jwt.secret");
        Date now = new Date();
        if (authenticate(credentials)) {
            return Jwts.builder().
                    setSubject(credentials.getEmail())
                    .claim("roles", "user")
                    .setIssuedAt(now)
                    .setExpiration(new Date(now.getTime() + 86400000))
                    .signWith(SignatureAlgorithm.HS256, secret)
                    .compact();
        } else {
            throw new ServiceException("Passwords do not match.");
        }
    }

    @Override
    public User get(String id) {
        User ret = super.get(id);
        if (ret != null) {
            ret.setPassword("");
            ret.setResetToken("");
        }
        return ret;
    }

    private User reveal(User user) {
        return super.get(user.getId());
    }

//    @Override
//    public void setEnvironment(Environment environment) {
//        this.env = environment;
//    }
}
