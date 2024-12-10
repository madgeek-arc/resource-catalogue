package gr.uoa.di.madgik.resourcecatalogue.config.dynamicproperties;

import org.springframework.context.ApplicationEvent;

public class PropertyChangeEvent extends ApplicationEvent {
    private final String propertyName;
    private final String newValue;

    public PropertyChangeEvent(Object source, String propertyName, String newValue) {
        super(source);
        this.propertyName = propertyName;
        this.newValue = newValue;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getNewValue() {
        return newValue;
    }
}

