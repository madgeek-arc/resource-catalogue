//package gr.uoa.di.madgik.resourcecatalogue.service;
//
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//public class ResourceIdCreatorTests {
//
//    private List<String> resourceTypes;
//
//    @BeforeEach
//    public void setUp() {
//        resourceTypes = Arrays.asList(
//                "catalogue",
//                "configuration_template",
//                "cti", //configuration_template_instance
//                "datasource",
//                "event",
//                "helpdesk",
//                "interoperability_record",
//                "monitoring",
//                "pro", //draft_provider
//                "ser", //draft_service
//                "provider",
//                "rir", //resource_interoperability_record
//                "service",
//                "training_resource",
//                "cur" //vocabulary_curation
//        );
//    }
//
//    @Test
//    public void testRandomness() {
//        ResourceIdCreator creator = new ResourceIdCreator();
//        int to = 100000; //TODO: fails on 1 mil, probably change library
//        for (String resourceType : resourceTypes) {
//            Set<String> ids = new HashSet<>();
//            for (int i = 0; i < to; i++) {
//                ids.add(creator.generate(resourceType));
//            }
//            Assertions.assertEquals(to, ids.size());
//        }
//    }
//
//    @Test
//    public void testPrefix() {
//        ResourceIdCreator creator = new ResourceIdCreator();
//        for (String resourceType : resourceTypes) {
//            String id = creator.generate(resourceType);
//            Assertions.assertEquals(resourceType.substring(0, 3), id.substring(0, 3));
//        }
//    }
//
//    @Test
//    public void testIdLength() {
//        ResourceIdCreator creator = new ResourceIdCreator();
//        for (String resourceType : resourceTypes) {
//            String id = creator.generate(resourceType);
//            Assertions.assertEquals(10, id.length());
//        }
//    }
//}
