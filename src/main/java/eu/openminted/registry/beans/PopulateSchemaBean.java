package eu.openminted.registry.beans;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.service.ResourceTypeService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Date;

/**
 * Populate the database with the known resourceTypes if not existing.
 * The json files for the resourceTypes are located in the resources/resourceTypes
 * Created by stefanos on 5/1/2017.
 */

//@Component
public class PopulateSchemaBean {
    @Autowired
    ResourceTypeService resourceTypeService;

//    @Autowired
//    RequestMappingHandlerAdapter adapter;

    private static String ANTPATH = "classpath:resourceTypes/*.json";

    private static Logger logger = Logger.getLogger(PopulateSchemaBean.class);

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        logger.info("Initializing schema");
        ClassLoader cl = this.getClass().getClassLoader();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
        Resource[] resources = resolver.getResources(ANTPATH) ;
        for (Resource resource: resources){
            addResourceType(resource);
        }
    }

    private void addResourceType(Resource type) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ResourceType resourceType = mapper.readValue(type.getInputStream(),ResourceType.class);
        ResourceType existing = resourceTypeService.getResourceType(resourceType.getName());
        if (existing == null) {
            resourceType.setCreationDate(new Date());
            resourceType.setModificationDate(new Date());
            logger.info("Adding resourceType " + resourceType.getName());
            resourceTypeService.addResourceType(resourceType);
        } else {
            logger.info("Existing resourceType " + resourceType.getName());
        }
    }

    @PreDestroy
    public void destroy() throws Exception {
        logger.info("Finished initialization");
    }
}
