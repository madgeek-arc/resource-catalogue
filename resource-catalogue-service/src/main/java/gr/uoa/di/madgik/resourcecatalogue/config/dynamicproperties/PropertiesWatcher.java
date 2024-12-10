package gr.uoa.di.madgik.resourcecatalogue.config.dynamicproperties;

import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;

public class PropertiesWatcher {

    private final Path secretPropertiesPath;

    public PropertiesWatcher(String filePath) {
        this.secretPropertiesPath = Paths.get(filePath);
    }

    public void watchFile(Runnable onFileChange) throws IOException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        secretPropertiesPath.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

        new Thread(() -> {
            while (true) {
                try {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.context().toString().equals(secretPropertiesPath.getFileName().toString())) {
                            onFileChange.run();
                        }
                    }
                    key.reset();
                } catch (Exception e) {
                    //TODO: log it
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        try (var input = Files.newInputStream(secretPropertiesPath)) {
            properties.load(input);
        }
        return properties;
    }
}