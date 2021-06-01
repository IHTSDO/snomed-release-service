package org.ihtsdo.buildcloud.config;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

import java.util.Properties;

public abstract class BaseConfiguration {

	private static final String HIBERNATE_DIALECT_PROPERTY_KEY = "hibernate.dialect";
	private static final String HIBERNATE_SHOW_SQL_PROPERTY_KEY = "hibernate.show_sql";
	private static final String HIBERNATE_SHOW_SQL_PROPERTY_VALUE = "false";
	private static final String HIBERNATE_CONNECTION_CHARSET_PROPERTY_KEY = "hibernate.connection.CharSet";
	private static final String HIBERNATE_CONNECTION_CHARSET_PROPERTY_VALUE = "utf8";
	private static final String HIBERNATE_CONNECTION_CHARACTER_ENCODING_PROPERTY_KEY = "hibernate.connection.characterEncoding";
	private static final String HIBERNATE_CONNECTION_CHARACTER_ENCODING_PROPERTY_VALUE = "utf8";
	private static final String HIBERNATE_CONNECTION_USE_UNICODE_PROPERTY_KEY = "hibernate.connection.useUnicode";
	private static final String HIBERNATE_CONNECTION_USE_UNICODE_PROPERTY_VALUE = "true";
	private static final String PACKAGES_TO_SCAN_VALUE = "org.ihtsdo.buildcloud.core.entity";

	protected BasicDataSource getBasicDataSource(final String driverClassName, final String url, final String username,
			final String password) {
		final BasicDataSource basicDataSource = new BasicDataSource();
		basicDataSource.setDriverClassName(driverClassName);
		basicDataSource.setUrl(url);
		basicDataSource.setUsername(username);
		basicDataSource.setPassword(password);
		return basicDataSource;
	}

	protected Properties getProperties(final String dialect) {
		final Properties properties = new Properties();
		properties.setProperty(HIBERNATE_DIALECT_PROPERTY_KEY, dialect);
		properties.setProperty(HIBERNATE_SHOW_SQL_PROPERTY_KEY, HIBERNATE_SHOW_SQL_PROPERTY_VALUE);
		properties.setProperty(HIBERNATE_CONNECTION_CHARSET_PROPERTY_KEY, HIBERNATE_CONNECTION_CHARSET_PROPERTY_VALUE);
		properties.setProperty(HIBERNATE_CONNECTION_CHARACTER_ENCODING_PROPERTY_KEY, HIBERNATE_CONNECTION_CHARACTER_ENCODING_PROPERTY_VALUE);
		properties.setProperty(HIBERNATE_CONNECTION_USE_UNICODE_PROPERTY_KEY, HIBERNATE_CONNECTION_USE_UNICODE_PROPERTY_VALUE);
		return properties;
	}

	protected LocalSessionFactoryBean getSessionFactory(final String driverClassName, final String url,
			final String username, final String password, final String dialect) {
		final LocalSessionFactoryBean localSessionFactoryBean = new LocalSessionFactoryBean();
		localSessionFactoryBean.setDataSource(getBasicDataSource(driverClassName, url, username, password));
		localSessionFactoryBean.setPackagesToScan(PACKAGES_TO_SCAN_VALUE);
		localSessionFactoryBean.setHibernateProperties(getProperties(dialect));
		return localSessionFactoryBean;
	}
}
