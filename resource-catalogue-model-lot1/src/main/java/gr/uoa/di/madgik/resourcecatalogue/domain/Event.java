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

package gr.uoa.di.madgik.resourcecatalogue.domain;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement
public class Event implements Identifiable {
    @XmlElement(required = true)
    private String id;
    @XmlElement(required = true)
    private long instant;
    @XmlElement(required = true)
    private String type;
    @XmlElement(required = true)
    private String user;
    @XmlElement(required = true)
    private String service;
    @XmlElement()
    private Float value;

    public Event() {
    }

    public Event(String type, String user, String service, Float value) {
        this.type = type;
        this.user = user;
        this.service = service;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", instant=" + instant +
                ", type='" + type + '\'' +
                ", user='" + user + '\'' +
                ", service='" + service + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public long getInstant() {
        return instant;
    }

    public void setInstant(long instant) {
        this.instant = instant;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public Float getValue() {
        return value;
    }

    public void setValue(Float value) {
        this.value = value;
    }

    public enum UserActionType {
        FAVOURITE("FAVOURITE"),
        RATING("RATING"),
        VISIT("VISIT"),
        ORDER("ORDER"),
        ADD_TO_PROJECT("ADD_TO_PROJECT");

        private final String type;

        UserActionType(final String type) {
            this.type = type;
        }

        public String getKey() {
            return type;
        }
    }
}