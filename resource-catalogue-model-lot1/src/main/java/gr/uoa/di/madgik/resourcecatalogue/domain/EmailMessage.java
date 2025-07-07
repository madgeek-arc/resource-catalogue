/**
 *Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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

@XmlType
@XmlRootElement
public class EmailMessage {

    @XmlElement
    @FieldValidation
    private String recipientEmail;

    @XmlElement
    @FieldValidation
    private String senderEmail;

    @XmlElement
    @FieldValidation
    private String senderName;

    @XmlElement
    @FieldValidation()
    private String subject;

    @XmlElement
    @FieldValidation
    private String message;

    public EmailMessage() {
    }

    public EmailMessage(String recipientEmail, String senderEmail, String senderName, String subject, String message) {
        this.recipientEmail = recipientEmail;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.subject = subject;
        this.message = message;
    }

    @Override
    public String toString() {
        return "EmailMessage{" +
                "recipientEmail='" + recipientEmail + '\'' +
                ", senderEmail='" + senderEmail + '\'' +
                ", senderName='" + senderName + '\'' +
                ", subject='" + subject + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
