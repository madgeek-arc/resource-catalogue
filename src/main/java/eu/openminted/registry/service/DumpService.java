package eu.openminted.registry.service;

import eu.openminted.registry.core.domain.Resource;

import java.io.File;
import java.util.List;

public interface DumpService {

	File bringAll();

	File bringResourceType(String resourceType);

	String getCurrentDate();

	void createDirectory(String name, List<Resource> resources);
}
