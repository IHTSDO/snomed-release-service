package org.ihtsdo.buildcloud.config;

import org.ihtsdo.context.OrderedPropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

@Configuration
public class OrderedPropertyPlaceholderConfigurerConfiguration {

	private static final String CLASSPATH_DATA_SERVICE_DEFAULTS_PROPERTIES_LOCATION = "classpath:application.properties";
	private static final String DATA_SERVICE_PROPERTIES_PATH_LOCATION = "+{dataServicePropertiesPath}";
	private static final String LOCAL_RVF_PROPERTY_KEY = "localRvf";
	private static final String LOCAL_RVF_PROPERTY_VALUE = "false";
	private static final String S3_OFFLINE_DIRECTORY_KEY = "s3.offline.directory";
	private static final String S3_OFFLINE_DIRECTORY_VALUE = "";
	private static final boolean IGNORE_RESOURCE_NOT_FOUND_VALUE = true;

	@Bean
	public OrderedPropertyPlaceholderConfigurer configurer2() {
		final OrderedPropertyPlaceholderConfigurer configurer = new OrderedPropertyPlaceholderConfigurer();
		configurer.setLocations(getLocations());
		configurer.setIgnoreResourceNotFound(IGNORE_RESOURCE_NOT_FOUND_VALUE);
		configurer.setProperties(getProperties());
		return configurer;
	}

	private ClassPathResource[] getLocations() {
		return new ClassPathResource[] {
				new ClassPathResource(CLASSPATH_DATA_SERVICE_DEFAULTS_PROPERTIES_LOCATION),
				new ClassPathResource(DATA_SERVICE_PROPERTIES_PATH_LOCATION)
		};
	}

	private Properties getProperties() {
		final Properties properties = new Properties();
		properties.setProperty(LOCAL_RVF_PROPERTY_KEY, LOCAL_RVF_PROPERTY_VALUE);
		properties.setProperty(S3_OFFLINE_DIRECTORY_KEY, S3_OFFLINE_DIRECTORY_VALUE);
		return properties;
	}
}
