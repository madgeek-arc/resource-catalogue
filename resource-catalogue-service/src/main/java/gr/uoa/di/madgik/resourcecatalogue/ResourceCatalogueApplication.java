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

package gr.uoa.di.madgik.resourcecatalogue;

import org.flowable.spring.boot.FlowableJpaAutoConfiguration;
import org.flowable.spring.boot.FlowableSecurityAutoConfiguration;
import org.flowable.spring.boot.eventregistry.EventRegistryAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        EventRegistryAutoConfiguration.class,
        FlowableJpaAutoConfiguration.class,
        // Flowable 7.1 references the removed Spring-Boot 3 security auto-config class.
        FlowableSecurityAutoConfiguration.class
})
public class ResourceCatalogueApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResourceCatalogueApplication.class, args);
    }

}
