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

package gr.uoa.di.madgik.resourcecatalogue.manager.pids;

import gr.uoa.di.madgik.resourcecatalogue.domain.AdapterBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.CatalogueBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.DatasourceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.DeployableApplicationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.InteroperabilityRecordBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.TrainingResourceBundle;

import java.util.Map;

/**
 * Maps a {@link Bundle}'s concrete class to its canonical resourceType key. Unlike deriving the
 * type from a Handle id prefix, this is unambiguous even when multiple resource types share the
 * same prefix (a Handle prefix is assigned per institution, not per resource type).
 */
final class BundleResourceTypes {

    private static final Map<Class<?>, String> TYPES = Map.ofEntries(
            Map.entry(OrganisationBundle.class, "organisation"),
            Map.entry(AdapterBundle.class, "adapter"),
            Map.entry(ServiceBundle.class, "service"),
            Map.entry(DatasourceBundle.class, "datasource"),
            Map.entry(InteroperabilityRecordBundle.class, "interoperability_record"),
            Map.entry(DeployableApplicationBundle.class, "deployable_application"),
            Map.entry(TrainingResourceBundle.class, "training_resource"),
            Map.entry(CatalogueBundle.class, "catalogue")
    );

    private BundleResourceTypes() {
    }

    static String resolve(Bundle bundle) {
        String resourceType = TYPES.get(bundle.getClass());
        if (resourceType == null) {
            throw new IllegalArgumentException("No resourceType mapped for bundle class " + bundle.getClass());
        }
        return resourceType;
    }
}
