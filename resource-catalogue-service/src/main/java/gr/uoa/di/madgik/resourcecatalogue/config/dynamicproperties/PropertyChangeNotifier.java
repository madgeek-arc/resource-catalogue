package gr.uoa.di.madgik.resourcecatalogue.config.dynamicproperties;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class PropertyChangeNotifier {
    private final ApplicationEventPublisher eventPublisher;

    public PropertyChangeNotifier(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void notifyPropertyChange(String propertyName, String newValue) {
        eventPublisher.publishEvent(new PropertyChangeEvent(this, propertyName, newValue));
    }
}
