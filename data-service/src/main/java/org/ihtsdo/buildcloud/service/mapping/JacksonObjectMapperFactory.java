package org.ihtsdo.buildcloud.service.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JacksonObjectMapperFactory {

	/**
	 * Creates the Jackson ObjectMapper with the options we want.
	 *
	 * @return
	 */
	public ObjectMapper createObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		return objectMapper;
	}

}
