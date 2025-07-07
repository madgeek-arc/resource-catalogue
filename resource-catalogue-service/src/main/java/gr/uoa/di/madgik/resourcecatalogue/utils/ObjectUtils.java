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

package gr.uoa.di.madgik.resourcecatalogue.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public class ObjectUtils {

    private ObjectUtils() {
    }

    private static final Logger logger = LoggerFactory.getLogger(ObjectUtils.class);

    public static <T> T clone(T object) {
        ObjectMapper objectMapper = new ObjectMapper();
        T deepCopy = null;
        try {
            String json = objectMapper.writeValueAsString(object);
            deepCopy = (T) objectMapper.readValue(json, object.getClass());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return deepCopy;
    }

    public static Object merge(Object existing, Object update) {
        if (!existing.getClass().isAssignableFrom(update.getClass())) {
            return existing;
        }

        Method[] methods = existing.getClass().getMethods();

        for (Method fromMethod : methods) {
            if (fromMethod.getDeclaringClass().equals(existing.getClass())
                    && fromMethod.getName().startsWith("get")) {

                String fromName = fromMethod.getName();
                String toName = fromName.replace("get", "set");

                try {
                    Method toMethod = existing.getClass().getMethod(toName, fromMethod.getReturnType());
                    Object value = fromMethod.invoke(update, (Object[]) null);
                    if (value != null) {
                        toMethod.invoke(existing, value);
                    }
                } catch (Exception e) {
                    logger.error("ERROR", e);
                }
            }
        }
        return existing;
    }
}
