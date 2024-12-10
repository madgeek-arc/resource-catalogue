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

    @Value("${secret.properties.path}")
    private String secretPath;

    @Autowired
    private ConfigurableEnvironment environment;
    @Autowired
    private PropertyChangeNotifier notifier;

    private PropertiesWatcher watcher;
    private Map<String, String> previousProperties = new HashMap<>();

    @PostConstruct
    public void init() throws IOException {
        String secretFilePath = secretPath;
        this.watcher = new PropertiesWatcher(secretFilePath);
        reloadSecretProperties();

        // Watch for changes
        watcher.watchFile(this::reloadSecretProperties);
    }

    private void reloadSecretProperties() {
        try {
            Properties secretProperties = watcher.loadProperties();

            Map<String, String> currentProperties = secretProperties.entrySet()
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