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
import gr.uoa.di.madgik.resourcecatalogue.domain.LoggingInfo;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gr.uoa.di.madgik.resourcecatalogue.utils.TestUtils.createOrganisationBundle;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the mail templates: each one the code asks for exists, none is left unused, and the ones that
 * changed render without leftovers.
 */
class MailTemplatesUnitTest {

    private static final Path MAIN = Path.of("src", "main");
    private static final Pattern TEMPLATE_NAME = Pattern.compile("\"([A-Za-z]+\\.ftl)\"");

    private Configuration cfg;
    private Map<String, Object> root;

    @BeforeEach
    void setUp() {
        cfg = new Configuration(Configuration.getVersion());
        cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "mail");
        root = new HashMap<>();
        root.put("project", "EOSC");
        root.put("endpoint", "https://example.org");
        root.put("registrationEmail", "registration@example.org");
        root.put("user", new User("id", "jane@example.org", "Jane", "Doe"));
        root.put("organisationPath", "11.1111%2Forg00");
    }

    private String render(String template) throws Exception {
        StringWriter out = new StringWriter();
        cfg.getTemplate(template).process(root, out);
        return out.toString();
    }

    private static TreeSet<String> referencedTemplates() throws IOException {
        TreeSet<String> names = new TreeSet<>();
        try (Stream<Path> files = Files.walk(MAIN.resolve("java"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher matcher = TEMPLATE_NAME.matcher(Files.readString(file));
                while (matcher.find()) {
                    names.add(matcher.group(1));
                }
            }
        }
        return names;
    }

    @Test
    void everyTemplateTheCodeAsksForExistsAndEveryTemplateIsUsed() throws IOException {
        TreeSet<String> existing;
        try (Stream<Path> files = Files.list(MAIN.resolve("resources").resolve("mail"))) {
            existing = files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".ftl"))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
        TreeSet<String> referenced = referencedTemplates();

        assertThat(referenced).as("templates the code asks for but that do not exist")
                .allMatch(existing::contains);
        assertThat(existing).as("templates nothing asks for").allMatch(referenced::contains);
    }

    @Test
    void organisationTemplateMailsNameTheTypeAndLinkToTheRightPlace() throws Exception {
        OrganisationBundle organisation = createOrganisationBundle();
        organisation.setTemplateStatus("approved template");
        root.put("bundle", organisation);
        root.put("resourceType", "Training Resource");
        root.put("resourceBundleName", "My Training");
        root.put("resourceBundleId", "11.1111/abc00");
        root.put("resourceDashboardPath", "training-resource-dashboard/11.1111%2Fabc00/history");

        String mail = render("organisationOnboardingForOrganisationAdmins.ftl");

        assertThat(mail).contains("as a new Training Resource")
                .contains("https://example.org/dashboard/11.1111%2Forg00/training-resource-dashboard/11.1111%2Fabc00/history")
                .doesNotContain("[Option").doesNotContain("[Attached");
    }

    @Test
    void rejectedTemplateMailHasNoPlaceholders() throws Exception {
        OrganisationBundle organisation = createOrganisationBundle();
        organisation.setTemplateStatus("rejected template");
        root.put("bundle", organisation);
        root.put("resourceType", "Adapter");
        root.put("resourceBundleName", "My Adapter");
        root.put("resourceBundleId", "11.1111/abc00");

        assertThat(render("organisationOnboardingForOrganisationAdmins.ftl"))
                .contains("has been rejected").contains("registration@example.org")
                .doesNotContain("[Option").doesNotContain("[Attached");
        assertThat(render("organisationOnboardingForPortalAdmins.ftl"))
                .contains("The Adapter: [My Adapter] - [11.1111/abc00]");
    }

    @Test
    void invalidUpdateMailWorksForAnyType() throws Exception {
        OrganisationBundle organisation = createOrganisationBundle();
        LoggingInfo update = new LoggingInfo();
        update.setComment("fixed it");
        organisation.setLatestUpdateInfo(update);
        root.put("bundle", organisation);
        root.put("resourceType", "Datasource");
        root.put("resourceBundleName", "My Datasource");
        root.put("resourceBundleId", "11.1111/abc00");

        assertThat(render("invalidUpdateForPortalAdmins.ftl"))
                .contains("The Datasource [My Datasource] - [11.1111/abc00]").contains("fixed it");
    }

    @Test
    void dailyDigestListsEveryTypeThatChanged() throws Exception {
        LoggingInfo log = new LoggingInfo();
        log.setType("onboard");
        log.setUserRole("admin");
        Map<String, Object> adapters = new HashMap<>();
        adapters.put("label", "Adapters");
        adapters.put("created", List.of("adapter-1"));
        adapters.put("updated", List.of());
        adapters.put("activity", Map.of("adapter-1", List.of(log)));
        Map<String, Object> datasources = new HashMap<>();
        datasources.put("label", "Datasources");
        datasources.put("created", List.of());
        datasources.put("updated", List.of("ds-1"));
        datasources.put("activity", Map.of());
        root.put("sections", List.of(adapters, datasources));

        assertThat(render("dailyDigestForPortalAdmins.ftl"))
                .contains("New Adapters:").contains("adapter-1").contains("Adapters Logging Info:")
                .contains("\"adapter-1\" got \"onboard\" by [admin]")
                .contains("Updated Datasources:").contains("ds-1")
                .doesNotContain("New Datasources");
    }
}
