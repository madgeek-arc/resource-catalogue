package eu.einfracentral.recdb;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EventsModel {
    @JsonProperty("label")
    public String label;
    @JsonProperty("nb_uniq_visitors")
    public int nb_uniq_visitors;
    @JsonProperty("nb_visits")
    public int nb_visits;
    @JsonProperty("nb_events")
    public int nb_events;
    @JsonProperty("nb_events_with_value")
    public int nb_events_with_value;
    @JsonProperty("sum_event_value")
    public int sum_event_value;
    @JsonProperty("min_event_value")
    public int min_event_value;
    @JsonProperty("max_event_value")
    public int max_event_value;
    @JsonProperty("avg_event_value")
    public int avg_event_value;
    @JsonProperty("Events_EventCategory")
    public String Events_EventCategory;
    @JsonProperty("Events_EventAction")
    public String Events_EventAction;
}
