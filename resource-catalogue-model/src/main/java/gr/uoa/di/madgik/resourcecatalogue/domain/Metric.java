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

import gr.uoa.di.madgik.resourcecatalogue.annotation.FieldValidation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.net.URL;
import java.util.Objects;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Metric {

    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private URL probe;

    @XmlElement
    @Schema
    @FieldValidation(nullable = true)
    private URL metric;

    public Metric() {
    }

    public Metric(URL probe, URL metric) {
        this.probe = probe;
        this.metric = metric;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Metric metric1 = (Metric) o;
        return Objects.equals(probe, metric1.probe) && Objects.equals(metric, metric1.metric);
    }

    @Override
    public int hashCode() {
        return Objects.hash(probe, metric);
    }

    @Override
    public String toString() {
        return "Metric{" +
                "probe=" + probe +
                ", metric=" + metric +
                '}';
    }

    public URL getProbe() {
        return probe;
    }

    public void setProbe(URL probe) {
        this.probe = probe;
    }

    public URL getMetric() {
        return metric;
    }

    public void setMetric(URL metric) {
        this.metric = metric;
    }
}
