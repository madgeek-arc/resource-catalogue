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
package gr.uoa.di.madgik.resourcecatalogue.onboarding;

/**
 * How resources of a given type are onboarded. Configured per resource type through
 * {@code onboarding.yml}, see {@link gr.uoa.di.madgik.resourcecatalogue.config.properties.OnboardingProperties}.
 */
public enum OnboardingPolicy {

    /**
     * Every resource goes through {@code pending} and needs approval. {@code templateStatus} is never touched.
     */
    REVIEW_ALL,

    /**
     * The Organisation's first resource is a template and goes {@code pending}. Once it is approved, the
     * Organisation's later resources are approved immediately.
     */
    REVIEW_FIRST,

    /**
     * Every resource is approved immediately.
     */
    REVIEW_NONE;

    /**
     * Value exposed to the onboarding workflows (BPMN) as the {@code onboardingPolicy} process variable.
     */
    public String workflowValue() {
        return name().toLowerCase().replace('_', '-');
    }
}
