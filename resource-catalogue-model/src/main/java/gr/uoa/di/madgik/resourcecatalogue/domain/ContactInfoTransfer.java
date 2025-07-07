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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.Objects;

@XmlRootElement
public class ContactInfoTransfer {
    @XmlElement(required = true)
    @Schema
    private String email;
    @XmlElement(required = true, defaultValue = "false")
    @Schema
    private Boolean acceptedTransfer;

    public ContactInfoTransfer() {
    }

    public ContactInfoTransfer(String email, boolean acceptedTransfer) {
        this.email = email;
        this.acceptedTransfer = acceptedTransfer;
    }

    @Override
    public String toString() {
        return "ContactInfoTransfer{" +
                "email='" + email + '\'' +
                ", acceptedTransfer=" + acceptedTransfer +
                '}';
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.toLowerCase() : null;
    }

    public Boolean getAcceptedTransfer() {
        return acceptedTransfer;
    }

    public void setAcceptedTransfer(Boolean acceptedTransfer) {
        this.acceptedTransfer = acceptedTransfer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContactInfoTransfer that = (ContactInfoTransfer) o;
        return acceptedTransfer == that.acceptedTransfer && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, acceptedTransfer);
    }
}
