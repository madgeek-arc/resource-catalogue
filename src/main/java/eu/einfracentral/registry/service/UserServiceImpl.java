package eu.einfracentral.registry.service;

import eu.einfracentral.domain.aai.User;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
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
public class UserServiceImpl<T> extends BaseGenericResourceCRUDServiceImpl<User> implements UserService, EnvironmentAware {

    private Environment env;

    private Properties jmp;

    private Session session;

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
            //update(ret); //TODO: Ask Stephanus about rollback error
        }
        return ret;
    }
    private Properties getConfig() {
        String[] propNames = new String[]{"mail.smtp.auth", "mail.smtp.host", "mail.smtp.password", "mail.smtp.port", "mail.smtp.socketFactory.class", "mail.smtp.socketFactory.port", "mail.smtp.starttls.enable", "mail.smtp.user"};
        Properties ret = new Properties();
        for (String prop : propNames) {
            String val = env.getProperty(prop);
            ret.setProperty(prop, val);
        }
        return ret;
    }

    public void sendMail(User user) {
        this.jmp = getConfig();
        this.session = Session.getDefaultInstance(jmp, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(jmp.getProperty("mail.smtp.user"), jmp.getProperty("mail.smtp.password"));
            }
        });
        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(jmp.getProperty("mail.smtp.user")));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(user.getEmail()));
            msg.setSubject(jmp.getProperty("mail.subject"));
            msg.setText(jmp.getProperty("mail.text") + user.getId());
            Transport transport = session.getTransport("smtp");
            transport.connect(jmp.getProperty("mail.smtp.host"), jmp.getProperty("mail.smtp.port"));
            transport.send(msg);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

//    @Override
//    public Browsing getAll(FacetFilter facetFilter) {
//        return new Browsing(0, 0, 0, new ArrayList<Order>(), new ArrayList<Facet>());
//    }

    @Override
    public User register(User user) {
        User ret = null;
        if (getUserByEmail(user.getEmail()) == null) {
            user.setId(UUID.randomUUID().toString());
            sendMail(user);
            ret = hashUser(user);
            add(ret, ParserService.ParserServiceTypes.JSON);
            ret.setPassword("");
            sendMail(ret);
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
        try {
            Resource foundResource = searchService.searchId(getResourceType(), new SearchService.KeyValue("email", email));
            User foundUser = parserPool.serialize(foundResource, typeParameterClass).get();
            return get(foundUser.getId());
        } catch (UnknownHostException | InterruptedException | ExecutionException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }
    }

    @Override
    public String getToken(User credentials) {
        Date now = new Date();
        if (authenticate(credentials)) {
            return Jwts.builder().
                    setSubject(credentials.getEmail())
                    .claim("roles", "user")
                    .setIssuedAt(now)
                    .setExpiration(new Date(now.getTime() + 86400000))
                    .signWith(SignatureAlgorithm.HS256, "secretkey")
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
        }
        return ret;
    }
    private User reveal(User user) {
        return super.get(user.getId());
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.env = environment;
    }
}
