package org.ihtsdo.buildcloud.service.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonObjectMapperFactory {

	/**
	 * Creates the Jackson {@code ObjectMapper} with the relevant options
	 * enabled - {@link SerializationFeature#INDENT_OUTPUT}.
	 *
	 * @return {@code ObjectMapper} with the relevant options enabled  -
	 * {@link SerializationFeature#INDENT_OUTPUT}.
	 */
	@Bean
	public ObjectMapper createObjectMapper() {
		return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
	}
}
