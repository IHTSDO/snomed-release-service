package org.ihtsdo.buildcloud.config;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BasicDataSourceConfiguration extends BaseConfiguration {

	@Bean(destroyMethod = "close")
	public BasicDataSource dataSource(@Value("${srs.jdbc.driverClassName}") final String driverClassName,
			@Value("${srs.jdbc.url}") final String url, @Value("${srs.jdbc.username}") final String username,
			@Value("${srs.jdbc.password}") final String password) {
		return getBasicDataSource(driverClassName, url, username, password);
	}
}
