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

package gr.uoa.di.madgik.resourcecatalogue.validators;

import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.annotation.*;
import gr.uoa.di.madgik.resourcecatalogue.domain.*;
import gr.uoa.di.madgik.resourcecatalogue.manager.ProviderManager;
import gr.uoa.di.madgik.resourcecatalogue.service.*;
import io.netty.channel.ChannelOption;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class FieldValidator {

    private static final Logger logger = LoggerFactory.getLogger(FieldValidator.class);

    private final VocabularyService vocabularyService;
    private final ProviderManager providerService;
    private final ServiceBundleService<ServiceBundle> serviceBundleService;
    private final TrainingResourceService trainingResourceService;
    private final DeployableServiceService deployableServiceService;
    private final CatalogueService catalogueService;
    private final InteroperabilityRecordService interoperabilityRecordService;
    private final AdapterService adapterService;

    private static final String MANDATORY_FIELD = "Field [%s] is mandatory.";
    private static final String NULL_OBJECT = "Attempt to validate null object..";

    private Deque<String> validationLocation;

    public FieldValidator(VocabularyService vocabularyService,
                          ProviderManager providerService,
                          @Lazy ServiceBundleService<ServiceBundle> serviceBundleService,
                          @Lazy TrainingResourceService trainingResourceService,
                          @Lazy CatalogueService catalogueService,
                          @Lazy InteroperabilityRecordService interoperabilityRecordService,
                          @Lazy DeployableServiceService deployableServiceService,
                          @Lazy AdapterService adapterService) {
        this.vocabularyService = vocabularyService;
        this.providerService = providerService;
        this.serviceBundleService = serviceBundleService;
        this.catalogueService = catalogueService;
        this.interoperabilityRecordService = interoperabilityRecordService;
        this.trainingResourceService = trainingResourceService;
        this.deployableServiceService = deployableServiceService;
        this.adapterService = adapterService;
    }

    private String getCurrentLocation() {
        return validationLocation.stream().filter(Objects::nonNull).collect(Collectors.joining("->"));
    }

    public void validate(Object o) throws IllegalAccessException {
        validationLocation = new ArrayDeque<>();
        validateFields(o);
        if (o.getClass().getSuperclass() != null && (o.getClass().getSuperclass().getCanonicalName().contains("gr.uoa.di.madgik.resourcecatalogue.domain.Bundle"))) {
            try {
                Field payload = Bundle.class.getDeclaredField("payload");
                payload.setAccessible(true);
                o = payload.get(o);
            } catch (NoSuchFieldException e) {
                logger.error("Could not find field 'payload'", e);
            }
            validateFields(o);
        }
    }

    private void validateFields(Object o) throws IllegalAccessException {
        if (o == null) {
            throw new ValidationException(NULL_OBJECT);
        }

        // get declared fields of class
        List<Field> declaredFields = new ArrayList<>(Arrays.asList(o.getClass().getDeclaredFields()));
        if (o instanceof ServiceBundle) {
            declaredFields.addAll(Arrays.asList(o.getClass().getSuperclass().getDeclaredFields()));
        }
        if (o instanceof TrainingResourceBundle) {
            declaredFields.addAll(Arrays.asList(o.getClass().getSuperclass().getDeclaredFields()));
        }
        if (o instanceof DeployableServiceBundle) {
            declaredFields.addAll(Arrays.asList(o.getClass().getSuperclass().getDeclaredFields()));
        }
        if (o instanceof InteroperabilityRecordBundle) {
            declaredFields.addAll(Arrays.asList(o.getClass().getSuperclass().getDeclaredFields()));
        }

        // validate every field
        for (Field field : declaredFields) {
            validationLocation.addLast(field.getName());
            validateField(field, o);
            validationLocation.removeLast();
        }
    }

    private void validateField(Field field, Object o) throws IllegalAccessException {
        if (o == null) { // parent object here
            throw new ValidationException(NULL_OBJECT);
        }

        // email validation
        if (field.getAnnotation(EmailValidation.class) != null) {
            validateEmail(field, o, field.getAnnotation(EmailValidation.class));
            return;
        }

        // phone validation
        if (field.getAnnotation(PhoneValidation.class) != null) {
            validatePhone(field, o, field.getAnnotation(PhoneValidation.class));
            return;
        }

        // check if FieldValidation annotation exists
        Annotation vocabularyValidation = field.getAnnotation(VocabularyValidation.class);
        Annotation geoLocationVocValidation = field.getAnnotation(GeoLocationVocValidation.class);
        FieldValidation annotation = field.getAnnotation(FieldValidation.class);
        if (vocabularyValidation != null && annotation == null) {
            annotation = vocabularyValidation.annotationType().getAnnotation(FieldValidation.class);
        }
        // region/countries validation
        if (geoLocationVocValidation != null && annotation == null) {
            annotation = geoLocationVocValidation.annotationType().getAnnotation(FieldValidation.class);
        }

        validateField(field, o, annotation);
    }

    private void validatePhone(Field field, Object o, PhoneValidation annotation) throws IllegalAccessException {
        field.setAccessible(true);
        o = field.get(o);
        if (annotation.nullable() && (o == null || o.equals(""))) {
            return;
        } else if (o == null) {
            throw new ValidationException(String.format(MANDATORY_FIELD, getCurrentLocation()));
        }
        Pattern phonePattern = Pattern.compile("^(((\\+)|(00))\\d{1,3}( )?)?((\\(\\d{3}\\))|\\d{3})[- .]?\\d{3}[- .]?\\d{4}$");
        if (!phonePattern.matcher(o.toString()).matches()) {
            throw new ValidationException(String.format("Field [%s]: The phone you provided '%s' is not valid.", getCurrentLocation(), o));
        }
    }

    private void validateEmail(Field field, Object o, EmailValidation annotation) throws IllegalAccessException {
        field.setAccessible(true);
        o = field.get(o);
        if (annotation.nullable() && (o == null || o.equals(""))) {
            return;
        } else if (o == null) {
            throw new ValidationException(String.format(MANDATORY_FIELD, getCurrentLocation()));
        }
        EmailValidator emailValidator = EmailValidator.getInstance();
        if (!emailValidator.isValid(o.toString())) {
            throw new ValidationException(String.format("Field [%s]: Email '%s' is not valid.", getCurrentLocation(), o));
        }
    }

    private void validateField(Field field, Object o, FieldValidation annotation) throws IllegalAccessException {
        if (o == null) {
            throw new ValidationException(NULL_OBJECT);
        }

        if (annotation != null) {

            field.setAccessible(true);

            Object fieldValue = field.get(o);
            Class clazz = null;
            if (fieldValue != null) {
                clazz = field.get(o).getClass();

                if (Collection.class.isAssignableFrom(fieldValue.getClass())) {
                    removeNullOrEmptyEntries((Collection) fieldValue);
                    field.set(o, fieldValue);
                } else if ((String.class.isAssignableFrom(fieldValue.getClass())
                        || URL.class.isAssignableFrom(fieldValue.getClass()))
                        && "".equals(fieldValue)) {
                    fieldValue = null;
                    field.set(o, fieldValue);
                }
            }

            if (!annotation.nullable() && isNullOrEmpty(fieldValue, clazz)) {
                throw new ValidationException(String.format(MANDATORY_FIELD, getCurrentLocation()));
            }

            validateMaxLength(field, fieldValue, annotation);
            validateUrlValidity(field, fieldValue);
            validateDuplicates(field, fieldValue);

            if (annotation.containsId()) {
                validateIds(field, fieldValue, annotation);
            } else if (fieldValue != null && fieldValue.getClass().getCanonicalName().startsWith("gr.uoa.di.madgik.")) {
                validateFields(fieldValue);
            } else if (fieldValue != null && Collection.class.isAssignableFrom(fieldValue.getClass())) {
                for (Object entry : ((Collection) fieldValue)) {
                    validateFields(entry);
                }
            }
        }
    }

    private boolean isNullOrEmpty(Object o, Class clazz) {
        if (o == null)
            return true;
        else if (String.class.equals(clazz) && "".equals((String) o))
            return true;
        else if (URL.class.equals(clazz) && "".equals(o.toString()))
            return true;
        else if (Collection.class.isAssignableFrom(clazz) && ((Collection) o).isEmpty())
            return true;
        return false;
    }

    private void validateMaxLength(Field field, Object o, FieldValidation annotation) {
        if (annotation.maxLength() > 0 && o != null) {
            Class clazz = o.getClass();
            if (String.class.equals(clazz) || URL.class.equals(clazz)) {
                String val = (String) o;
                if (val.length() > annotation.maxLength()) {
                    throw new ValidationException(String.format("Max length for field '%s' is %s characters.",
                            field.getName(), annotation.maxLength()));
                }
            }
            // if given object is a collection, apply 'validateMaxLength' to all entries
            else if (Collection.class.isAssignableFrom(clazz) && !((Collection) o).isEmpty()) {
                for (Object entry : ((Collection) o)) {
                    validateMaxLength(field, entry, annotation);
                }
            }
        }
    }

    private void validateUrlValidity(Field field, Object o) {
        if (o != null) {
            Class clazz = o.getClass();
            if (URL.class.equals(clazz)) {
                URL url = (URL) o;
                validateUrl(field, url);
            } else if (ArrayList.class.equals(clazz) && !((ArrayList) o).isEmpty() && URL.class.equals(((ArrayList) o).getFirst().getClass())) {
                for (int i = 0; i < ((ArrayList) o).size(); i++) {
                    URL url = (URL) ((ArrayList) o).get(i);
                    validateUrl(field, url);
                }
            }
        }
    }

    public void validateUrl(Field field, URL urlForValidation) {
        try {
            String cleanedUrlString = urlForValidation.toString().replaceAll("\\s", "%20");
            URI uri = new URL(cleanedUrlString).toURI(); // validate and clean

            // add timeout
            ReactorClientHttpConnector connector = new ReactorClientHttpConnector(
                    HttpClient.create()
                            .followRedirect(true)
                            .responseTimeout(Duration.ofSeconds(5))
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            );
            WebClient webClient = WebClient.builder()
                    .clientConnector(connector)
                    .build();

            ClientResponse response = webClient.get()
                    .uri(uri)
                    .exchangeToMono(Mono::just)
                    .block();

            HttpStatusCode statusCode = response.statusCode();
            if (!statusCode.is2xxSuccessful()) {
                String fieldName = (field != null) ? field.getName() : "unknown";
                throw new ValidationException(
                        String.format("Field [%s]: the URL you provided '%s' responded with error code: %d",
                                fieldName, urlForValidation, statusCode.value()));
            }
        } catch (URISyntaxException | MalformedURLException | WebClientResponseException | WebClientRequestException e) {
            throw new ValidationException("Failed to validate URL: " + urlForValidation);
        }
    }

    // TODO: find a better way to get resources by id
    private void validateIds(Field field, Object o, FieldValidation annotation) {
        if (o != null && annotation.containsId()) {
            if (Collection.class.isAssignableFrom(o.getClass())) {
                for (Object entry : ((Collection) o)) {
                    validateIds(field, entry, annotation);
                }
            } else if (annotation.idClasses().length > 0) { //TODO: revit the way we validate for linkedResources
                LinkedResource linkedResource = (LinkedResource) o;
                boolean found = false;
                Class<?>[] classes = annotation.idClasses();
                List<String> classSimpleNames = new ArrayList<>();
                classSimpleNames.add("Guideline");
                for (Class<?> clazz : classes) {
                    classSimpleNames.add(clazz.getSimpleName());
                }
                for (String classSimpleName : classSimpleNames) {
                    found = getResource(classSimpleName, linkedResource.getId());
                    if (found) {
                        break;
                    }
                }
                if (!found) {
                    throw new ValidationException(
                            String.format("Field [%s]: Should contain the ID of an existing Service " +
                                    "or Guideline.", field.getName()));
                }
            } else if (String.class.equals(o.getClass())) {
                try {
                    if (annotation.containsResourceId()) {
                        ServiceBundle serviceBundle = serviceBundleService.getOrElseReturnNull(o.toString());
                        TrainingResourceBundle trainingResourceBundle = trainingResourceService.getOrElseReturnNull(o.toString());
                        if (serviceBundle == null && trainingResourceBundle == null) {
                            throw new ValidationException(
                                    String.format("Field [%s]: Should contain the ID of an existing Service " +
                                            "or Training Resource", field.getName()));
                        }
                    } else if (Vocabulary.class.equals(annotation.idClass())) {
                        Vocabulary voc = vocabularyService.get(o.toString());
                        VocabularyValidation vocabularyValidation = field.getAnnotation(VocabularyValidation.class);
                        GeoLocationVocValidation geoLocationVocValidation = field.getAnnotation(GeoLocationVocValidation.class);
                        if (vocabularyValidation != null) {
                            if (voc == null || Vocabulary.Type.fromString(voc.getType()) != vocabularyValidation.type()) {
                                throw new ValidationException(
                                        String.format("Field [%s]: Should contain the ID of a type '%s' Vocabulary",
                                                field.getName(), vocabularyValidation.type()));
                            }
                        }
                        // region/countries validation
                        if (geoLocationVocValidation != null) {
                            if (voc == null || (Vocabulary.Type.fromString(voc.getType()) != geoLocationVocValidation.region()
                                    && Vocabulary.Type.fromString(voc.getType()) != geoLocationVocValidation.country())) {
                                throw new ValidationException(
                                        String.format("Field [%s]: Should contain the ID of either one of the types '%s' or '%s' Vocabularies",
                                                field.getName(), geoLocationVocValidation.region(), geoLocationVocValidation.country()));
                            }
                        }
                    } else if (Provider.class.equals(annotation.idClass())
                            && providerService.get(o.toString()) == null) { //FIXME catalogueID
                        throw new ValidationException(
                                String.format("Field [%s]: Should contain the ID of an existing Provider",
                                        field.getName()));
                    } else if ((gr.uoa.di.madgik.resourcecatalogue.domain.Service.class.equals(annotation.idClass())
                            || ServiceBundle.class.equals(annotation.idClass()))
                            && serviceBundleService.get(o.toString()) == null) {
                        throw new ValidationException(
                                String.format("Field [%s]: Should contain the ID of an existing Service",
                                        field.getName()));
                    } else if ((TrainingResource.class.equals(annotation.idClass())
                            || TrainingResourceBundle.class.equals(annotation.idClass()))
                            && trainingResourceService.get(o.toString()) == null) {
                        throw new ValidationException(
                                String.format("Field [%s]: Should contain the ID of an existing Training Resource",
                                        field.getName()));
                    } else if ((DeployableService.class.equals(annotation.idClass())
                            || DeployableServiceBundle.class.equals(annotation.idClass()))
                            && deployableServiceService.get(o.toString()) == null) {
                        throw new ValidationException(
                                String.format("Field [%s]: Should contain the ID of an existing Deployable Service",
                                        field.getName()));
                    } else if ((Catalogue.class.equals(annotation.idClass())
                            || CatalogueBundle.class.equals(annotation.idClass()))
                            && catalogueService.get(o.toString()) == null) {
                        throw new ValidationException(
                                String.format("Field [%s]: Should contain the ID of an existing Catalogue",
                                        field.getName()));
                    } else if ((InteroperabilityRecord.class.equals(annotation.idClass())
                            || InteroperabilityRecordBundle.class.equals(annotation.idClass()))
                            && interoperabilityRecordService.get(o.toString()) == null) {
                        throw new ValidationException(
                                String.format("Field [%s]: Should contain the ID of an existing InteroperabilityRecord",
                                        field.getName()));
                    }
                } catch (ResourceException | ResourceNotFoundException e) {
                    throw new ValidationException(
                            String.format("Field [%s]: %s with ID '%s' does not exist.", field.getName(),
                                    annotation.idClass().getSimpleName(), o));
                }
            }
        }
    }

    private void removeNullOrEmptyEntries(Collection collection) {
        if (collection != null) {
            for (Iterator i = collection.iterator(); i.hasNext(); ) {
                Object entry = i.next();
                // if elements are of type String or URL
                if (entry != null) {
                    if ((String.class.isAssignableFrom(entry.getClass())
                            || URL.class.isAssignableFrom(entry.getClass()))
                            && "".equals(entry.toString())) {
                        i.remove(); // remove empty string entries ("")
                    }
                } else {
                    i.remove(); // remove null entries
                }
            }
        }
    }

    private void validateDuplicates(Field field, Object o) {
        Set<String> duplicateEntries = new HashSet<>();
        String subField = field.toString().substring(field.toString().lastIndexOf(".") + 1);
        if (o != null) {
            Class clazz = o.getClass();
            if (ArrayList.class.equals(clazz)) {
                for (int i = 0; i < ((ArrayList) o).size(); i++) {
                    if (!duplicateEntries.add(((ArrayList) o).get(i).toString())) {
                        throw new ValidationException(String.format("Field [%s]: Duplicate value found '%s'", subField, ((ArrayList) o).get(i).toString()));
                    }
                }
            }
        }
    }

    private boolean getResource(String className, Object o) {
        return switch (className) {
            case "Service" -> serviceBundleService.getOrElseReturnNull(o.toString()) != null;
            case "TrainingResource" -> trainingResourceService.getOrElseReturnNull(o.toString()) != null;
            case "InteroperabilityRecord", "Guideline" -> interoperabilityRecordService.getOrElseReturnNull(o.toString()) != null;
            default -> false;
        };
    }
}
