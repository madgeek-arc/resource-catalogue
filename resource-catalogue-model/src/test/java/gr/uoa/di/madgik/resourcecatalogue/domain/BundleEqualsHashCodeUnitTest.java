/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

package gr.uoa.di.madgik.resourcecatalogue.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * {@link Bundle} and its subclasses hand-write {@code equals()}/{@code hashCode()} rather than
 * generating them, so nothing enforces that a newly added field actually gets wired into either
 * method. This test catches that omission: for every declared field of every Bundle subclass, it
 * checks that changing only that field on an otherwise-identical instance flips both equals() and
 * hashCode(). If it doesn't, the field was forgotten.
 */
class BundleEqualsHashCodeUnitTest {

    private static Stream<Class<? extends Bundle>> bundleClasses() {
        return Stream.of(
                Bundle.class,
                OrganisationBundle.class,
                DatasourceBundle.class,
                ServiceBundle.class,
                CatalogueBundle.class,
                TrainingResourceBundle.class,
                InteroperabilityRecordBundle.class,
                AdapterBundle.class,
                DeployableApplicationBundle.class,
                ConfigurationTemplateBundle.class,
                ConfigurationTemplateInstanceBundle.class,
                ResourceInteroperabilityRecordBundle.class
        );
    }

    @ParameterizedTest
    @MethodSource("bundleClasses")
    void everyFieldParticipatesInEqualsAndHashCode(Class<? extends Bundle> bundleClass) throws Exception {
        for (Field field : allInstanceFields(bundleClass)) {
            field.setAccessible(true);

            Bundle a = bundleClass.getDeclaredConstructor().newInstance();
            Bundle b = bundleClass.getDeclaredConstructor().newInstance();
            assertEquals(a, b, () -> "Baseline " + bundleClass.getSimpleName() + " instances should be equal before mutation");

            Object[] valuePair = valuesFor(field);
            field.set(a, valuePair[0]);
            field.set(b, valuePair[1]);

            assertNotEquals(a, b, () -> "Field '" + field.getName() + "' declared on " + field.getDeclaringClass().getSimpleName()
                    + " is not used in " + bundleClass.getSimpleName() + ".equals() -- did you forget to add it?");
            assertNotEquals(a.hashCode(), b.hashCode(), () -> "Field '" + field.getName() + "' declared on "
                    + field.getDeclaringClass().getSimpleName() + " is not used in " + bundleClass.getSimpleName()
                    + ".hashCode() -- did you forget to add it?");
        }
    }

    private static List<Field> allInstanceFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())) {
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    /**
     * Two distinct, otherwise-default values for the given field's declared type, used to mutate
     * one of an otherwise-identical pair of instances. Add a case here if Bundle or a subclass
     * gains a field of a type not already covered.
     */
    private static Object[] valuesFor(Field field) {
        Class<?> type = field.getType();
        if (type == boolean.class || type == Boolean.class) {
            return new Object[]{true, false};
        }
        if (type == String.class) {
            return new Object[]{"value-a", "value-b"};
        }
        if (type == LinkedHashMap.class) {
            LinkedHashMap<String, Object> a = new LinkedHashMap<>();
            a.put("k", "a");
            LinkedHashMap<String, Object> b = new LinkedHashMap<>();
            b.put("k", "b");
            return new Object[]{a, b};
        }
        if (type == List.class) {
            return new Object[]{List.of(loggingInfo("a")), List.of(loggingInfo("b"))};
        }
        if (type == Metadata.class) {
            return new Object[]{metadata("a"), metadata("b")};
        }
        if (type == Identifiers.class) {
            return new Object[]{identifiers("a"), identifiers("b")};
        }
        if (type == LoggingInfo.class) {
            return new Object[]{loggingInfo("a"), loggingInfo("b")};
        }
        throw new IllegalStateException("No test value factory for field '" + field.getName() + "' of type "
                + type.getSimpleName() + " -- add one to BundleEqualsHashCodeUnitTest.valuesFor()");
    }

    private static Metadata metadata(String suffix) {
        Metadata metadata = new Metadata();
        metadata.setRegisteredBy("user-" + suffix);
        return metadata;
    }

    private static Identifiers identifiers(String suffix) {
        Identifiers identifiers = new Identifiers();
        identifiers.setPid("pid-" + suffix);
        return identifiers;
    }

    private static LoggingInfo loggingInfo(String suffix) {
        LoggingInfo loggingInfo = new LoggingInfo();
        loggingInfo.setComment("comment-" + suffix);
        return loggingInfo;
    }
}
