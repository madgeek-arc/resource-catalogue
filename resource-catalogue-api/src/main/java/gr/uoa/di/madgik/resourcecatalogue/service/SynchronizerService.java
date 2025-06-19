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

package gr.uoa.di.madgik.resourcecatalogue.service;

public interface SynchronizerService<T> {

    /**
     * Synchronize resource addition on a different host
     *
     * @param t resource
     */
    void syncAdd(T t);

    /**
     * Synchronize resource update on a different host
     *
     * @param t resource
     */
    void syncUpdate(T t);

    /**
     * Synchronize resource deletion on a different host
     *
     * @param t resource
     */
    void syncDelete(T t);

    /**
     * Synchronize resource verification on a different host
     *
     * @param t resource
     */
    void syncVerify(T t);
}
