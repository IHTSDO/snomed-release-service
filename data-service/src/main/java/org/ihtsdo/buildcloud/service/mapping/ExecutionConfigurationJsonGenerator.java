package org.ihtsdo.buildcloud.service.mapping;

import org.codehaus.jackson.map.ObjectMapper;
import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Package;
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
	private static final String PACKAGES = "packages";
	private static final String INPUT_FILES = "inputFiles";
	private static final String PRODUCT = "product";
	private static final String EXTENSION = "extension";
	private static final String RELEASE_CENTRE = "releaseCentre";
	private static final String VERSION = "version";

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
		config.put(PACKAGES, getPackagesConfig(build.getPackages()));
		config.put(PRODUCT, getConfig(build.getProduct()));
		return config;
	}

	private Map<String, Object> getConfig(Product product) {
		Map<String, Object> config = new LinkedHashMap<>();
		config.put(ID, product.getBusinessKey());
		config.put(NAME, product.getName());
		config.put(EXTENSION, getConfig(product.getExtension()));
		return config;
	}

	private Map<String, Object> getConfig(Extension extension) {
		Map<String, Object> config = new LinkedHashMap<>();
		config.put(ID, extension.getBusinessKey());
		config.put(NAME, extension.getName());
		config.put(RELEASE_CENTRE, getConfig(extension.getReleaseCentre()));
		return config;
	}

	private Map<String, Object> getConfig(ReleaseCentre releaseCentre) {
		Map<String, Object> config = new LinkedHashMap<>();
		config.put(ID, releaseCentre.getBusinessKey());
		config.put(NAME, releaseCentre.getName());
		config.put(SHORT_NAME, releaseCentre.getShortName());
		return config;
	}

	private List<Map> getPackagesConfig(List<Package> packages) {
		List<Map> configList = new ArrayList<>();
		for (Package aPackage : packages) {
			configList.add(getConfig(aPackage));
		}
		return configList;
	}

	private Map<String, Object> getConfig(Package aPackage) {
		Map<String, Object> config = new LinkedHashMap<>();
		config.put(ID, aPackage.getBusinessKey());
		config.put(NAME, aPackage.getName());
		config.put(INPUT_FILES, getInputFilesConfig(aPackage.getInputFiles()));
		return config;
	}

	private List<Map> getInputFilesConfig(List<InputFile> inputFiles) {
		List<Map> configList = new ArrayList<>();
		for (InputFile inputFile : inputFiles) {
			configList.add(getConfig(inputFile));
		}
		return configList;
	}

	private Map<String, Object> getConfig(InputFile inputFile) {
		Map<String, Object> config = new LinkedHashMap<>();
		config.put(ID, inputFile.getBusinessKey());
		config.put(NAME, inputFile.getName());
		config.put(VERSION, inputFile.getVersion());
		return config;
	}

}
