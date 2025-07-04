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

package gr.uoa.di.madgik.resourcecatalogue.domain;

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.Objects;

public class ProviderRequest implements Identifiable {

    @FieldValidation
    private String id;

    @FieldValidation
    private EmailMessage message;

    @FieldValidation
    private String date;

    @FieldValidation(containsId = true, idClass = Provider.class)
    private String providerId;

    @FieldValidation
    private boolean isRead;

    public ProviderRequest() {
    }

    public ProviderRequest(String id, EmailMessage message, String date, String providerId, boolean isRead) {
        this.id = id;
        this.message = message;
        this.date = date;
        this.providerId = providerId;
        this.isRead = isRead;
    }

    @Override
    public String toString() {
        return "ProviderRequest{" +
                "id='" + id + '\'' +
                ", message=" + message +
                ", date=" + date +
                ", providerId='" + providerId + '\'' +
                ", status=" + isRead +
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

    public EmailMessage getMessage() {
        return message;
    }

    public void setMessage(EmailMessage message) {
        this.message = message;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        this.isRead = read;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProviderRequest that = (ProviderRequest) o;
        return isRead == that.isRead && Objects.equals(id, that.id) && Objects.equals(message, that.message) && Objects.equals(date, that.date) && Objects.equals(providerId, that.providerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, message, date, providerId, isRead);
    }
}
