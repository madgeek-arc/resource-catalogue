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

import java.util.List;

/**
 * The FDO-profile fields common to a resource's PID record, extracted once from
 * {@code Bundle.getPayload()} and shared between the Handle record builders ({@link PidIssuer})
 * and the FDO profile validator ({@link PidRecordValidator}). Organisation records leave
 * {@code publishingDate}/{@code type}/{@code resourceOwner} {@code null}, since Organisations have
 * none of their own.
 */
record PidFields(
        String name,
        String description,
        String publishingDate,
        String type,
        String nodePID,
        String resourceOwner,
        List<String> publicContacts) {
}
