package org.ihtsdo.buildcloud.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.aws.autoconfigure.context.ContextInstanceDataAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {
		ContextInstanceDataAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class}
		)
@TestConfiguration
@ComponentScan("org.ihtsdo.buildcloud.*")
public class DataServiceTestConfig extends DataServiceConfig {

	// TODO To disable liquibase during tests
//	@Override
//	public SpringLiquibase liquibase(@Value("${srs.jdbc.driverClassName}") final String driverClassName,
//	                                 @Value("${srs.jdbc.url}") final String url, @Value("${srs.jdbc.username}") final String username,
//	                                 @Value("${srs.jdbc.password}") final String password,
//	                                 @Value("${srs.environment.shortname}") final String shortname) {
//		final SpringLiquibase springLiquibase = new SpringLiquibase();
//		springLiquibase.setDataSource(getBasicDataSource(driverClassName, url, username, password));
//		springLiquibase.setContexts(shortname);
//		springLiquibase.setShouldRun(false);
//		return springLiquibase;
//	}
}
