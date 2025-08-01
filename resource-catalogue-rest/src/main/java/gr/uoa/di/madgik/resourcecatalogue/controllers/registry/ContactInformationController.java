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

package gr.uoa.di.madgik.resourcecatalogue.controllers.registry;

import gr.uoa.di.madgik.resourcecatalogue.service.ContactInformationService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Hidden
@Profile("beyond")
@RestController
@RequestMapping({"contactInformation"})
@Tag(name = "contact information", description = "Operations about contact information transfer")
public class ContactInformationController {

    private final ContactInformationService contactInformationService;

    public ContactInformationController(ContactInformationService contactInformationService) {
        this.contactInformationService = contactInformationService;
    }

    @Operation(summary = "Returns a list of Catalogues and Providers the user has accepted his/her information transfer.")
    @GetMapping(path = "getMy", produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<String> getMy(@Parameter(hidden = true) Authentication authentication) {
        return contactInformationService.getMy(authentication);
    }

    //    @PutMapping(path = "updateContactInfoTransfer", produces = {MediaType.APPLICATION_JSON_VALUE})
    public void updateContactInfoTransfer(@RequestParam boolean acceptedTransfer,
                                          @Parameter(hidden = true) Authentication authentication) {
        contactInformationService.updateContactInfoTransfer(acceptedTransfer, authentication);
    }
}
