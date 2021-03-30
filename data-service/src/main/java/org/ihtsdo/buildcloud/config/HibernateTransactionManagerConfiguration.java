package org.ihtsdo.buildcloud.config;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

@Configuration
@ConfigurationProperties
public class HibernateTransactionManagerConfiguration extends BaseConfiguration {

	@Bean(name = "sessionFactory")
	public LocalSessionFactoryBean sessionFactory(@Value("${srs.jdbc.driverClassName}") final String driverClassName,
			@Value("${srs.jdbc.url}") final String url, @Value("${srs.jdbc.username}") final String username,
			@Value("${srs.jdbc.password}") final String password, @Value("${srs.hibernate.dialect}") final String dialect) {
		return getSessionFactory(driverClassName, url, username, password, dialect);
	}

	@Bean
	public HibernateTransactionManager transactionManager(@Value("${srs.jdbc.driverClassName}") final String driverClassName,
			@Value("${srs.jdbc.url}") final String url, @Value("${srs.jdbc.username}") final String username,
			@Value("${srs.jdbc.password}") final String password, @Value("${srs.hibernate.dialect}") final String dialect,
			@Autowired SessionFactory sessionFactory) {
		final HibernateTransactionManager transactionManager = new HibernateTransactionManager();
		transactionManager.setDataSource(getBasicDataSource(driverClassName, url, username, password));
		transactionManager.setSessionFactory(sessionFactory);
		return transactionManager;
	}
}
