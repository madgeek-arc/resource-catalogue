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
package gr.uoa.di.madgik.resourcecatalogue.unit;

import freemarker.template.Configuration;
import freemarker.template.Template;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createServiceBundle;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders the mails sent for resources whose type uses the {@code review-all} onboarding policy.
 */
class ResourceOnboardingMailTemplateUnitTest {

    private Configuration cfg;
    private Map<String, Object> root;
    private ServiceBundle service;

    @BeforeEach
    void setUp() {
        cfg = new Configuration(Configuration.getVersion());
        cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "mail");
        service = createServiceBundle();
        service.getService().put("name", "My Service");
        root = new HashMap<>();
        root.put("project", "EOSC");
        root.put("endpoint", "https://example.org");
        root.put("registrationEmail", "registration@example.org");
        root.put("bundle", service);
        root.put("user", new User("id", "jane@example.org", "Jane", "Doe"));
        root.put("resourceType", "Service");
        root.put("resourceAdminListPath", "provider/resource/all");
    }

    private String render(String template, String status) throws Exception {
        service.setStatus(status);
        Template ftl = cfg.getTemplate(template);
        StringWriter out = new StringWriter();
        ftl.process(root, out);
        return out.toString();
    }

    @Test
    void providerAdminsAreToldWhereTheirApplicationStands() throws Exception {
        assertThat(render("resourceOnboardingForOrganisationAdmins.ftl", "pending"))
                .contains("My Service").contains("Service").contains("will be reviewed");
        assertThat(render("resourceOnboardingForOrganisationAdmins.ftl", "approved")).contains("has been approved");
        assertThat(render("resourceOnboardingForOrganisationAdmins.ftl", "rejected"))
                .contains("has been rejected").contains("registration@example.org");
    }

    @Test
    void portalAdminsAreToldWhatToReview() throws Exception {
        assertThat(render("resourceOnboardingForPortalAdmins.ftl", "pending"))
                .contains("Jane Doe").contains("My Service").contains("https://example.org/provider/resource/all");
        assertThat(render("resourceOnboardingForPortalAdmins.ftl", "approved")).contains("has been approved");
        assertThat(render("resourceOnboardingForPortalAdmins.ftl", "rejected")).contains("has been rejected");
    }
}
