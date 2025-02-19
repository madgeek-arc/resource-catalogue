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

package gr.uoa.di.madgik.resourcecatalogue.dto;

public class OpenAIREMetrics {

    private int pageViews;
    private int totalDownloads;
    private int totalOpenaireDownloads;
    private int totalViews;
    private int totalOpenaireViews;

    public OpenAIREMetrics() {
    }

    public OpenAIREMetrics(int pageViews, int totalDownloads, int totalOpenaireDownloads, int totalViews, int totalOpenaireViews) {
        this.pageViews = pageViews;
        this.totalDownloads = totalDownloads;
        this.totalOpenaireDownloads = totalOpenaireDownloads;
        this.totalViews = totalViews;
        this.totalOpenaireViews = totalOpenaireViews;
    }

    public int getPageViews() {
        return pageViews;
    }

    public void setPageViews(int pageViews) {
        this.pageViews = pageViews;
    }

    public int getTotalDownloads() {
        return totalDownloads;
    }

    public void setTotalDownloads(int totalDownloads) {
        this.totalDownloads = totalDownloads;
    }

    public int getTotalOpenaireDownloads() {
        return totalOpenaireDownloads;
    }

    public void setTotalOpenaireDownloads(int totalOpenaireDownloads) {
        this.totalOpenaireDownloads = totalOpenaireDownloads;
    }

    public int getTotalViews() {
        return totalViews;
    }

    public void setTotalViews(int totalViews) {
        this.totalViews = totalViews;
    }

    public int getTotalOpenaireViews() {
        return totalOpenaireViews;
    }

    public void setTotalOpenaireViews(int totalOpenaireViews) {
        this.totalOpenaireViews = totalOpenaireViews;
    }
}
