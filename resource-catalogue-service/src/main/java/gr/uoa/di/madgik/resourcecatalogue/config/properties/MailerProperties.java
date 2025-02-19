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

package gr.uoa.di.madgik.resourcecatalogue.config.properties;

import org.springframework.boot.autoconfigure.mail.MailProperties;


public class MailerProperties extends MailProperties {

    private String from;
    private boolean auth = true;
    private boolean ssl = true;

    public String getFrom() {
        return from;
    }

    public MailerProperties setFrom(String from) {
        this.from = from;
        return this;
    }

    public boolean isAuth() {
        return auth;
    }

    public MailerProperties setAuth(boolean auth) {
        this.auth = auth;
        return this;
    }

    public boolean isSsl() {
        return ssl;
    }

    public MailerProperties setSsl(boolean ssl) {
        this.ssl = ssl;
        return this;
    }
}
