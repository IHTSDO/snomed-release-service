package org.ihtsdo.buildcloud.config;

import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchemaFactoryConfiguration {

	@Bean
	public SchemaFactory schemaFactory() {
		return new SchemaFactory();
	}
}
