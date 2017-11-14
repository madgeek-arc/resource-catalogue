package eu.einfracentral.registry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openminted.registry.core.domain.Value;
import java.util.*;
import org.junit.Test;

/**
 * Created by pgl on 10/11/17.
 */
public class SerializeTest {
    @Test
    public void serTest() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<Value, Value> map = new HashMap<>();
        map.put(new Value("asd", 4), new Value("asd", 4));
        System.out.println(objectMapper.writeValueAsString(map));
    }
}
