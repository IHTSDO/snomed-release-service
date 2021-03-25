package org.ihtsdo.buildcloud.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringLiquibaseConfiguration extends BaseConfiguration {

	private static final String CHANGE_LOG_PATH = "classpath:org/ihtsdo/srs/db/changelog/db.changelog-master.xml";
	@Bean
	public SpringLiquibase liquibase(@Value("${srs.jdbc.driverClassName}") final String driverClassName,
			@Value("${srs.jdbc.url}") final String url, @Value("${srs.jdbc.username}") final String username,
			@Value("${srs.jdbc.password}") final String password, @Value("${srs.environment.shortname}") final String shortname) {
		final SpringLiquibase springLiquibase = new SpringLiquibase();
		springLiquibase.setDataSource(getBasicDataSource(driverClassName, url, username, password));
		springLiquibase.setChangeLog(CHANGE_LOG_PATH);
		springLiquibase.setContexts(shortname);
		return springLiquibase;
	}
}
