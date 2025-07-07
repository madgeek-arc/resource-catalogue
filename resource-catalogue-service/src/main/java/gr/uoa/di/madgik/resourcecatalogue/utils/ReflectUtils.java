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

import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectUtils {

    private static final Logger logger = LoggerFactory.getLogger(ReflectUtils.class);

    public static void setId(@NotNull Class<?> clazz, @NotNull Object resource, @NotNull String id) {
        try {
            Method setId = clazz.getDeclaredMethod("setId");
            setId.invoke(resource, id);
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            logger.warn("Could not find 'setId' method in class [{}]", clazz.getName());
            setIdField(clazz, resource, id);
        }
    }

    public static String getId(@NotNull Class<?> clazz, @NotNull Object resource) throws NoSuchMethodException, InvocationTargetException, NoSuchFieldException {
        String id = null;
        try {
            Method getId = clazz.getDeclaredMethod("getId");
            id = (String) getId.invoke(resource);
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            logger.warn("Could not find 'getId' method in class [{}]", clazz.getName());
            getIdField(clazz, resource);
        } catch (InvocationTargetException e) {
            throw e;
        }
        return id;
    }

    private static void setIdField(@NotNull Class<?> clazz, @NotNull Object resource, @NotNull String id) {
        try {
            Field idField = clazz.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(resource, id);
        } catch (NoSuchFieldException e) {
            logger.error("Could not find 'id' field in class [{}]", clazz.getName());
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static String getIdField(@NotNull Class<?> clazz, @NotNull Object resource) throws NoSuchFieldException {
        String id = null;
        try {
            Field idField = clazz.getDeclaredField("id");
            idField.setAccessible(true);
            id = (String) idField.get(resource);
        } catch (NoSuchFieldException e) {
            logger.error("Could not find 'id' field in class [{}]", clazz.getName());
            throw e;
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(), e);
        }
        return id;
    }

    private ReflectUtils() {
    }
}
