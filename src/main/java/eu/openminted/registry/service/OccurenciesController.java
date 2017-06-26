package eu.openminted.registry.service;

import eu.openminted.registry.core.domain.Occurencies;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@RestController
public class OccurenciesController {


    @Autowired
    ResourceService resourceService;

    @Autowired
    ResourceTypeService resourceTypeService;

    @Autowired
    SearchService searchService;

    @RequestMapping(value = "/occurencies/{resourceType}/", method = RequestMethod.GET, headers =
            "Accept=application/json")
    public ResponseEntity<String> getResourceType(@PathVariable("resourceType") String resourceType) {

        ResponseEntity<String> responseEntity;

//	    	ResourceType resourceTypeClass = resourceTypeService.getResourceType(resourceType);
//	    	if(resourceTypeClass!=null){
//		    	Map<String,Map<String,String>> overallValues = new HashMap<String,Map<String,String>>();
//		    	for(int y=0;y<resourceTypeClass.getIndexFields().size();y++){
//		    		Map<String,String> values = new HashMap<String,String>();
//		    		Paging paging = null;
//		    		try {
//		    			String[] value = new String[1];
//		    			value[1] =  resourceTypeClass.getIndexFields().get(y).getName();
//						paging = searchService.search(resourceType, "*", 0, 0, value);
//					} catch (ServiceException e) {
//						responseEntity = new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
//						return responseEntity;
//					}
//		    		for(int i=0;i<paging.getOccurencies().size();i++){
//		    			JSONObject json = new JSONObject(paging.getOccurencies().get(i).toString());
//		    			values.put(json.getString("value"), json.getInt("count")+"");
//			    	}
//		    		overallValues.put(resourceTypeClass.getIndexFields().get(y).getName(), values);
//		    	}
//		    	Occurencies occurencies = new Occurencies();
//		    	occurencies.setResourceType(resourceType);
//		    	occurencies.setValues(overallValues);
//		    	
//		    	responseEntity = new ResponseEntity<String>(Utils.objToJson(occurencies),HttpStatus.ACCEPTED);
//	    	}else{
        responseEntity = new ResponseEntity<String>("{\"message\":\"resource type not found\"", HttpStatus.NO_CONTENT);
//	    	}
        return responseEntity;
    }

    @RequestMapping(value = "/occurencies/", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity<String> getOccurencies() {

        ResponseEntity<String> responseEntity;
        ArrayList<Occurencies> occurencies = new ArrayList<Occurencies>();
//	    	
//	    	List<ResourceType> resourceTypes= resourceTypeService.getAllResourceType();
//	    	if(resourceTypes!=null){
//		    	for(int j=0;j<resourceTypes.size();j++){
//			    	Map<String,Map<String,String>> overallValues = new HashMap<String,Map<String,String>>();
//			    	ResourceType resourceType = resourceTypes.get(j);
//			    	for(int y=0;y<resourceType.getIndexFields().size();y++){
//			    		Map<String,String> values = new HashMap<String,String>();
//			    		Paging paging = null;
//			    		try {
//			    			String[] value = new String[1];
//			    			value[1] =  resourceType.getIndexFields().get(y).getName();
//							paging = searchService.search(resourceTypes.get(j).getName(), "*", 0, 0,value);
//						} catch (ServiceException e) {
//							responseEntity = new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
//							return responseEntity;
//						}
//			    		for(int i=0;i<paging.getOccurencies().size();i++){
//			    			JSONObject json = new JSONObject(paging.getOccurencies().get(i).toString());
//			    			values.put(json.getString("value"), json.getInt("count")+"");
//				    	}
//			    		overallValues.put(resourceType.getIndexFields().get(y).getName(), values);
//			    	}
//			    	Occurencies occurency = new Occurencies();
//			    	occurency.setResourceType(resourceTypes.get(j).getName());
//			    	occurency.setValues(overallValues);
//			    	occurencies.add(occurency);
//		    	}
//	    	}else{
//	    		responseEntity = new ResponseEntity<String>("{\"message\":\"No resource types available\"",HttpStatus
// .NO_CONTENT);
//	    	}
        responseEntity = new ResponseEntity<String>(Utils.objToJson(occurencies), HttpStatus.ACCEPTED);


        return responseEntity;
    }

}
