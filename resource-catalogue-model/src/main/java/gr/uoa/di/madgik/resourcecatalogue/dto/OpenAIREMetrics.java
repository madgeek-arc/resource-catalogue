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
