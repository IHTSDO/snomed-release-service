package org.ihtsdo.buildcloud.config;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class PropertyPlaceholderConfigurerConfiguration {

	private static final String SYSTEM_PROPERTY_MODE_OVERRIDE = "SYSTEM_PROPERTIES_MODE_OVERRIDE";
	private static final String PLACEHOLDER_PREFIX = "+{";
	private static final String DATA_SERVICE_PROPERTIES_PATH_KEY = "dataServicePropertiesPath";
	private static final String CLASSPATH_DATA_SERVICE_PROPERTIES_VALUE = "classpath:data-service.properties";
	private static final boolean IGNORE_UNRESOLVABLE_PLACEHOLDER_VALUE = true;

	@Bean
	public PropertyPlaceholderConfigurer configurer1() {
		final PropertyPlaceholderConfigurer configurer1 = new PropertyPlaceholderConfigurer();
		configurer1.setSystemPropertiesModeName(SYSTEM_PROPERTY_MODE_OVERRIDE);
		configurer1.setPlaceholderPrefix(PLACEHOLDER_PREFIX);
		configurer1.setProperties(getProperties());
		configurer1.setIgnoreUnresolvablePlaceholders(IGNORE_UNRESOLVABLE_PLACEHOLDER_VALUE);
		return configurer1;
	}

	private Properties getProperties() {
		final Properties properties = new Properties();
		properties.setProperty(DATA_SERVICE_PROPERTIES_PATH_KEY, CLASSPATH_DATA_SERVICE_PROPERTIES_VALUE);
		return properties;
	}
}
