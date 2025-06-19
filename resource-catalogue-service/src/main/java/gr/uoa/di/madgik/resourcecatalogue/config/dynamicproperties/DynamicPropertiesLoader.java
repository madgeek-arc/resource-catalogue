/**
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

package gr.uoa.di.madgik.resourcecatalogue.config.dynamicproperties;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Configuration
public class DynamicPropertiesLoader {

    private static final Logger logger = LoggerFactory.getLogger(DynamicPropertiesLoader.class);

    @Value("${dynamic.properties.path}")
    private String path;

    @Autowired
    private ConfigurableEnvironment environment;
    @Autowired
    private PropertyChangeNotifier notifier;

    private PropertiesWatcher watcher;
    private Map<String, String> previousProperties = new HashMap<>();

    @PostConstruct
    public void init() throws IOException {
        if (!path.isBlank()) {
            String filePath = path;
            this.watcher = new PropertiesWatcher(filePath);
            reloadDynamicProperties();

            // Watch for changes
            watcher.watchFile(this::reloadDynamicProperties);
        }
    }

    private void reloadDynamicProperties() {
        try {
            Properties dynamicProperties = watcher.loadProperties();

            Map<String, String> currentProperties = dynamicProperties.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            e -> String.valueOf(e.getKey()),
                            e -> String.valueOf(e.getValue())
                    ));

            // Notify changes for each updated property
            currentProperties.forEach((key, newValue) -> {
                String oldValue = previousProperties.get(key);
                if (oldValue == null || !oldValue.equals(newValue)) {
                    notifier.notifyPropertyChange(key, newValue);
                }
            });

            // Notify removal of properties
            previousProperties.keySet().stream()
                    .filter(key -> !currentProperties.containsKey(key))
                    .forEach(key -> notifier.notifyPropertyChange(key, null));

            // Update the Environment
            environment.getPropertySources().addFirst(new MapPropertySource("dynamicProperties", new HashMap<>(currentProperties)));

            // Update the previousProperties reference
            previousProperties = currentProperties;

            logger.info("Successfully reloaded properties");
        } catch (IOException e) {
            logger.error("Failed to reload properties file.", e);
        }
    }
}