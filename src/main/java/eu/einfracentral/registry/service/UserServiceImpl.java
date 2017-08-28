package eu.einfracentral.registry.service;

import eu.einfracentral.domain.aai.User;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ParserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Date;
import java.util.Properties;
import java.util.UUID;

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
    public void activate(String token) {
        User ret = get(token);
        if (ret.getJoin_date() != null) {
            ret.setJoin_date(new Date().toString());
        }
    }

    @Override
    public Browsing getAll(FacetFilter facetFilter) {
        throw new Error("Security");
    }

//    public void sendMail(User user) {
//        SimpleMailMessage email = new SimpleMailMessage();
//        email.setTo(user.getEmail());
//        email.setSubject("[eInfraCentral] Activate your account");
//        email.setText("Please visit http://einfracentral.eu/eic-registry/user/activate/" + user.getId());
//        email.setFrom("test.espas@gmail.com");
//        email.setReplyTo("test.espas@gmail.com");
//        JavaMailSenderImpl sender = new JavaMailSenderImpl();
//        sender.setHost("smtp.gmail.com");
//        sender.setPort(465);
//        sender.setUsername("test.espas@gmail.com");
//        sender.setPassword("s.a.g.a.p.w");
//        Properties jmp = new Properties();
//        jmp.setProperty("mail.transport.protocol", "ssl");
//        jmp.setProperty("mail.smtp.auth", "true");
//        jmp.setProperty("mail.smtp.starttls.enable", "true");
//        sender.setJavaMailProperties(jmp);
//        sender.send(email);
//    }
//
    @Override
    public void register(User user) {
        user.setId(UUID.randomUUID().toString());
        add(user, ParserService.ParserServiceTypes.JSON);
    }

    @Override
    public String getToken(User user) {
        Date now = new Date();
        String ret = Jwts.builder()
                .setSubject(user.getUsername())
                .claim("roles", "user")
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + 86400000 ))
                .signWith(SignatureAlgorithm.HS256, "secretkey")
                .compact();
        return ret;
    }
//
//    @Override
//    public void register(String userAsXML) {
//        Resource ret = new Resource();
//        ret.setPayload(userAsXML);
//        ret.setCreationDate(new Date());
//        ret.setModificationDate(new Date());
//        ret.setPayloadFormat("xml");
//        ret.setResourceType(getResourceType());
//        ret.setVersion("not_set");
//        resourceService.addResource(ret);
//
//        get
//
//        sendMail(user);
//    }
}