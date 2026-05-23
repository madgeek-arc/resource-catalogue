/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

package gr.uoa.di.madgik.resourcecatalogue.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "node")
@Validated
public class NodeProperties {

    @NotNull
    @NotEmpty
    private String name;

    @NestedConfigurationProperty
    @Valid
    private Pid pid = new Pid();

    @NestedConfigurationProperty
    @Valid
    private Registry registry = new Registry();

    public String getName() {
        return name;
    }

    public NodeProperties setName(String name) {
        this.name = name;
        return this;
    }

    public Pid getPid() {
        return pid;
    }

    public NodeProperties setPid(Pid pid) {
        this.pid = pid;
        return this;
    }

    public Registry getRegistry() {
        return registry;
    }

    public NodeProperties setRegistry(Registry registry) {
        this.registry = registry;
        return this;
    }

    public static class Pid {
        @NotNull
        @NotEmpty
        private String value;
        private boolean fixed = true;

        public String getValue() {
            return value;
        }

        public Pid setValue(String value) {
            this.value = value;
            return this;
        }

        public boolean isFixed() {
            return fixed;
        }

        public Pid setFixed(boolean fixed) {
            this.fixed = fixed;
            return this;
        }
    }

    public static class Registry {

        @NotNull
        @NotEmpty
        private String url;

        @NotNull
        @NotEmpty
        private String key;

        public String getUrl() {
            return url;
        }

        public Registry setUrl(String url) {
            this.url = url;
            return this;
        }

        public String getKey() {
            return key;
        }

        public Registry setKey(String key) {
            this.key = key;
            return this;
        }
    }
}
