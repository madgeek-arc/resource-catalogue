package eu.einfracentral.registry.service;

import eu.einfracentral.registry.manager.UserManager;
import java.io.IOException;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import org.junit.Test;

public class UserManagerTests {
    private static final HashMap<String, String> users = new HashMap<>();
    private static final int iterationCount = -1;
    private static final Random r = new SecureRandom();
    private static final byte[] salt = new byte[8];
    private static final String base = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<user xmlns=\"http://einfracentral.eu\">\n" +
            "<email>%s@eic</email>\n" +
            "<id>%s</id>\n" +
            "<iterationCount>%s</iterationCount>\n" +
            "<joinDate>%s</joinDate>\n" +
            "<password>%s</password>\n" +
//            "<salt>%s</salt>\n" +
            "</user>";

    @Test
    public void makeUsers() throws IOException {
        //users.put("username", "password");
        for (Map.Entry<String, String> user : users.entrySet()) {
            r.nextBytes(salt);
            Files.write(Paths.get(String.format("../eic-data/transformed/user.res/%s.xml", user.getKey())),
                        String.format(base,
                                      user.getKey(),
                                      user.getKey(),
                                      iterationCount,
                                      new Date().toString(),
//                                      new String(UserManager.hashPass(user.getValue().toCharArray(), salt, iterationCount)),
                                      new String(Base64.getEncoder().encode(salt))).getBytes());
        }
    }
}
