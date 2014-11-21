package org.ihtsdo.buildcloud.service.mapping;

import org.codehaus.jackson.map.ObjectMapper;
import org.ihtsdo.buildcloud.entity.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

public class ExecutionConfigurationJsonGenerator {

	@Autowired
	private ObjectMapper objectMapper;
	private static final String ID = "id";

	private static final String CREATION_TIME = "creationTime";
	private static final String BUILD = "build";
	private static final String NAME = "name";
	private static final String SHORT_NAME = "shortName";
	private static final String INPUT_FILES = "inputFiles";
	private static final String RELEASE_CENTER = "releaseCenter";

	public String getJsonConfig(Execution execution) throws IOException {
		Map<String, Object> config = getConfig(execution);
		StringWriter writer = new StringWriter();
		objectMapper.writeValue(writer, config);
		return writer.toString();
	}

	private Map<String, Object> getConfig(Execution execution) {
		Map<String, Object> config = new LinkedHashMap<>();
		config.put(ID, execution.getId());
		config.put(CREATION_TIME, execution.getCreationTime());
		config.put(BUILD, getConfig(execution.getBuild()));
		return config;
	}

	private Map<String, Object> getConfig(Build build) {
		Map<String, Object> config = new LinkedHashMap<>();
		config.put(ID, build.getBusinessKey());
		config.put(NAME, build.getName());
		config.put(ID, build.getBusinessKey());
		config.put(NAME, build.getName());
		config.put(INPUT_FILES, build.getInputFiles());
		config.put(RELEASE_CENTER, getConfig(build.getReleaseCenter()));
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
