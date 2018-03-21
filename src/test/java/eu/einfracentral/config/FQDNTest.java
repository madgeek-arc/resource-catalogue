package eu.einfracentral.config;

import java.net.*;
import org.junit.Test;

/**
 * Created by pgl on 21/03/18.
 */
public class FQDNTest {
    @Test
    public void test() throws UnknownHostException {
        System.out.println(InetAddress.getLocalHost().getHostName());
        System.out.println(InetAddress.getLocalHost().getCanonicalHostName());
    }
}