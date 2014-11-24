package org.ihtsdo.buildcloud.service.mapping;

import org.codehaus.jackson.map.ObjectMapper;
import org.ihtsdo.buildcloud.entity.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

public class BuildConfigurationJsonGenerator {

	@Autowired
	private ObjectMapper objectMapper;
	private static final String ID = "id";

	private static final String CREATION_TIME = "creationTime";
	private static final String BUILD = "product";
	private static final String NAME = "name";
	private static final String SHORT_NAME = "shortName";
	private static final String INPUT_FILES = "inputFiles";
	private static final String RELEASE_CENTER = "releaseCenter";

	public String getJsonConfig(Build build) throws IOException {
		Map<String, Object> config = getConfig(build);
		StringWriter writer = new StringWriter();
		objectMapper.writeValue(writer, config);
		return writer.toString();
	}

	private Map<String, Object> getConfig(Build build) {
		Map<String, Object> config = new LinkedHashMap<>();
		config.put(ID, build.getId());
		config.put(CREATION_TIME, build.getCreationTime());
		config.put(BUILD, getConfig(build.getProduct()));
		return config;
	}

	private Map<String, Object> getConfig(Product product) {
		Map<String, Object> config = new LinkedHashMap<>();
		config.put(ID, product.getBusinessKey());
		config.put(NAME, product.getName());
		config.put(ID, product.getBusinessKey());
		config.put(NAME, product.getName());
		config.put(INPUT_FILES, product.getInputFiles());
		config.put(RELEASE_CENTER, getConfig(product.getReleaseCenter()));
		return config;
	}

	private Map<String, Object> getConfig(ReleaseCenter releaseCenter) {
		Map<String, Object> config = new LinkedHashMap<>();
		config.put(ID, releaseCenter.getBusinessKey());
		config.put(NAME, releaseCenter.getName());
		config.put(SHORT_NAME, releaseCenter.getShortName());
		return config;
	}

}
