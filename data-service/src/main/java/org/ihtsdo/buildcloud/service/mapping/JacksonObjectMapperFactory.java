package org.ihtsdo.buildcloud.service.mapping;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

public class JacksonObjectMapperFactory {

	/**
	 * Creates the Jackson ObjectMapper with the options we want.
	 * @return
	 */
	public ObjectMapper createObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
		return objectMapper;
	}

}
