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

package gr.uoa.di.madgik.resourcecatalogue.controllers.lot1;

import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("crud")
@RestController
@RequestMapping(path = "interoperability-records")
@Tag(name = "interoperability records")
public class InteroperabilityRecordCrudController extends ResourceCrudController<InteroperabilityRecordBundle> {

    public InteroperabilityRecordCrudController(GenericResourceService genericResourceService) {
        super(genericResourceService);
    }

    @Override
    protected String getResourceTypeName() {
        return "interoperability_record";
    }
}