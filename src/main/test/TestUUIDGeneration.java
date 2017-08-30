import org.junit.Test;

import java.util.Base64;
import java.util.UUID;

/**
 * Created by pgl on 30/08/17.
 */
public class TestUUIDGeneration {

    @Test
    public void test2() throws Exception {

        String uuid = UUID.randomUUID().toString();
        String encoded = new String(Base64.getEncoder().encode(uuid.getBytes()));
        String decoded = new String(Base64.getDecoder().decode(encoded));

        System.err.println(uuid);
        System.err.println(encoded);
        System.err.println(decoded);
    }
}
