package org.ihtsdo.buildcloud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

@Configuration
public class LocalSessionFactoryBeanConfiguration extends BaseConfiguration {

	@Bean
	public LocalSessionFactoryBean sessionFactory(@Value("${srs.jdbc.driverClassName}") final String driverClassName,
			@Value("${srs.jdbc.url}") final String url, @Value("${srs.jdbc.username}") final String username,
			@Value("${srs.jdbc.password}") final String password, @Value("${srs.hibernate.dialect}") final String dialect) {
		return getSessionFactory(driverClassName, url, username, password, dialect);
	}
}
