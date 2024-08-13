package gr.uoa.di.madgik.resourcecatalogue.domain;

//BeanUtils.getProperty(resourceToAdd,"getId") to get id, if I ever drop Identifiable due to w/e error
public interface Identifiable {

    String getId();

    void setId(String s);
}
