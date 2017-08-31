package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Facet;
import eu.einfracentral.domain.Order;
import eu.einfracentral.domain.aai.User;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by pgl on 07/08/17.
 */
@org.springframework.stereotype.Service("userService")
public class UserServiceImpl<T> extends BaseGenericResourceCRUDServiceImpl<User> implements UserService {

    public UserServiceImpl() {
        super(User.class);
    }


    @Override
    public String getResourceType() {
        return "einfrauser";
    }


    @Override
    public User activate(String id) {
        User ret = get(id);
        if (ret.getJoin_date() != null) {
            ret.setJoin_date(new Date().toString());
        }
        return ret;
    }

    public void sendMail(User user) {
        System.err.println("Please visit http://vereniki.athenarc.gr:8080/eic-registry/user/activate/" + user.getId() + "/");
//        SimpleMailMessage email = new SimpleMailMessage();
//        email.setTo(user.getEmail());
//        email.setSubject("[eInfraCentral] Activate your account");
//        email.setText("Please visit http://einfracentral.eu/eic-registry/user/activate/" +encoded+"/");
//        email.setFrom("test.espas@gmail.com");
//        email.setReplyTo("test.espas@gmail.com");
//        JavaMailSenderImpl sender = new JavaMailSenderImpl();
//        sender.setHost("smtp.gmail.com");
//        sender.setPort(465);
//        sender.setUsername("test.espas@gmail.com");
//        sender.setPassword("s.a.g.a.p.w");
//        Properties jmp = new Properties();
//        jmp.setProperty("mail.transport.protocol", "smtp");
//        jmp.setProperty("mail.smtp.auth", "true");
//        jmp.setProperty("mail.smtp.starttls.enable", "true");
//        sender.setJavaMailProperties(jmp);
//        sender.send(email);
    }

//    @Override
//    public Browsing getAll(FacetFilter facetFilter) {
//        return new Browsing(0, 0, 0, new ArrayList<Order>(), new ArrayList<Facet>());
//    }

    @Override
    public User register(User user) {
        user.setId(UUID.randomUUID().toString());
        add(user, ParserService.ParserServiceTypes.JSON);
        sendMail(user);
        return get(user.getId());
    }


    @Override
    public boolean authenticate(User credentials) {
        User actual = getUserByEmail(credentials.getEmail());
        return actual.getPassword().equals(credentials.getPassword());
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
            ret.setConfirmPassword("");
        }
        return ret;
    }
}
