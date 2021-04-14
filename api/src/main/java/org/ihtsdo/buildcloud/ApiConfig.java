package org.ihtsdo.buildcloud;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.autoconfigure.context.ContextInstanceDataAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@SpringBootApplication(exclude = {
		ContextInstanceDataAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class}
		)
@Configuration
@PropertySources({
		@PropertySource(value = "classpath:api.properties"),
		@PropertySource(value = "file:${srsConfigLocation}/api.properties", ignoreResourceNotFound=true)})
@EnableConfigurationProperties
public class ApiConfig {
}
