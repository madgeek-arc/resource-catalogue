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

package gr.uoa.di.madgik.resourcecatalogue.service.sync;

import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class TrainingResourceSync extends AbstractSyncService<TrainingResource> {

    @Autowired
    public TrainingResourceSync(@Value("${sync.host:}") String host,
                                @Value("${sync.token.filepath:}") String filename,
                                @Value("${sync.enable:false}") boolean enabled,
                                WebClient.Builder webClientBuilder) {
        super(host, filename, enabled, webClientBuilder);
    }

    @Override
    protected String getController() {
        return "/trainingResource";
    }
}
