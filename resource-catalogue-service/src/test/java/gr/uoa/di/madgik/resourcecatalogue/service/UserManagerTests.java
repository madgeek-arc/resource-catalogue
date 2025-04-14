/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.resourcecatalogue.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;

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
            "</user>";

    @Test
    public void makeUsers() throws IOException {
        for (Map.Entry<String, String> user : users.entrySet()) {
            r.nextBytes(salt);
            Files.write(Paths.get(String.format("../eic-data/transformed/user.res/%s.xml", user.getKey())),
                    String.format(base,
                            user.getKey(),
                            user.getKey(),
                            iterationCount,
                            new Date(),
                            new String(Base64.getEncoder().encode(salt))).getBytes());
        }
    }
}
