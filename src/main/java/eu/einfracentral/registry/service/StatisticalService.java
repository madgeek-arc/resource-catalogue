package eu.einfracentral.registry.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service("statisticalService")
public interface StatisticalService{

    Map<String, Float> averageRatingByService(int serviceId);

}
