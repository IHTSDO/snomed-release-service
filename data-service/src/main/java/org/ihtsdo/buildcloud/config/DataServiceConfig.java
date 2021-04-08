package org.ihtsdo.buildcloud.config;

import com.amazonaws.auth.BasicAWSCredentials;
import liquibase.integration.spring.SpringLiquibase;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.hibernate.SessionFactory;
import org.ihtsdo.context.OrderedPropertyPlaceholderConfigurer;
import org.ihtsdo.otf.dao.s3.OfflineS3ClientImpl;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.S3ClientFactory;
import org.ihtsdo.otf.dao.s3.S3ClientImpl;
import org.ihtsdo.otf.dao.s3.helper.S3ClientHelper;
import org.ihtsdo.otf.jms.MessagingHelper;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.jms.config.SimpleJmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

@Configuration
@PropertySources({
		@PropertySource(value = "classpath:application.properties"),
		@PropertySource(value = "file:${data.service.config.location}/application.properties", ignoreResourceNotFound=true)})
@Primary
public class DataServiceConfig extends BaseConfiguration {

	private static final String CLASSPATH_DATA_SERVICE_DEFAULTS_PROPERTIES_LOCATION = "classpath:application.properties";
	private static final String DATA_SERVICE_PROPERTIES_PATH_LOCATION = "+{dataServicePropertiesPath}";
	private static final String LOCAL_RVF_PROPERTY_KEY = "localRvf";
	private static final String LOCAL_RVF_PROPERTY_VALUE = "false";
	private static final String S3_OFFLINE_DIRECTORY_KEY = "s3.offline.directory";
	private static final String S3_OFFLINE_DIRECTORY_VALUE = "";
	private static final String SYSTEM_PROPERTY_MODE_OVERRIDE = "SYSTEM_PROPERTIES_MODE_OVERRIDE";
	private static final String PLACEHOLDER_PREFIX = "+{";
	private static final String DATA_SERVICE_PROPERTIES_PATH_KEY = "dataServicePropertiesPath";
	private static final String CLASSPATH_DATA_SERVICE_PROPERTIES_VALUE = "classpath:data-service.properties";
	private static final String CHANGE_LOG_PATH = "classpath:org/ihtsdo/srs/db/changelog/db.changelog-master.xml";

	private static final boolean IGNORE_RESOURCE_NOT_FOUND_VALUE = true;
	private static final boolean IGNORE_UNRESOLVABLE_PLACEHOLDER_VALUE = true;

	private S3ClientFactory s3ClientFactory;

	@Bean
	public DelegatingSecurityContextAsyncTaskExecutor securityContextAsyncTaskExecutor(
			@Value("${delegate.security.context.async.task.executor.thread.pool.size}") final int threadPoolSize) {
		return new DelegatingSecurityContextAsyncTaskExecutor(getAsyncTaskExecutor(threadPoolSize));
	}

	@Bean
	public AsyncTaskExecutor getAsyncTaskExecutor(final int threadPoolSize) {
		final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
		threadPoolTaskExecutor.setCorePoolSize(threadPoolSize);
		return threadPoolTaskExecutor;
	}

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

	@Bean
	public OrderedPropertyPlaceholderConfigurer configurer2() {
		final OrderedPropertyPlaceholderConfigurer configurer = new OrderedPropertyPlaceholderConfigurer();
		configurer.setLocations(getLocations());
		configurer.setIgnoreResourceNotFound(IGNORE_RESOURCE_NOT_FOUND_VALUE);
		configurer.setProperties(getProperties2());
		return configurer;
	}

	private ClassPathResource[] getLocations() {
		return new ClassPathResource[] {
				new ClassPathResource(CLASSPATH_DATA_SERVICE_DEFAULTS_PROPERTIES_LOCATION),
				new ClassPathResource(DATA_SERVICE_PROPERTIES_PATH_LOCATION)
		};
	}

	private Properties getProperties2() {
		final Properties properties = new Properties();
		properties.setProperty(LOCAL_RVF_PROPERTY_KEY, LOCAL_RVF_PROPERTY_VALUE);
		properties.setProperty(S3_OFFLINE_DIRECTORY_KEY, S3_OFFLINE_DIRECTORY_VALUE);
		return properties;
	}

	@Bean
	public PropertyPlaceholderConfigurer configurer1() {
		final PropertyPlaceholderConfigurer configurer1 = new PropertyPlaceholderConfigurer();
		configurer1.setSystemPropertiesModeName(SYSTEM_PROPERTY_MODE_OVERRIDE);
		configurer1.setPlaceholderPrefix(PLACEHOLDER_PREFIX);
		configurer1.setProperties(getProperties1());
		configurer1.setIgnoreUnresolvablePlaceholders(IGNORE_UNRESOLVABLE_PLACEHOLDER_VALUE);
		return configurer1;
	}

	private Properties getProperties1() {
		final Properties properties = new Properties();
		properties.setProperty(DATA_SERVICE_PROPERTIES_PATH_KEY,
				CLASSPATH_DATA_SERVICE_PROPERTIES_VALUE);
		return properties;
	}

	@Bean
	public S3ClientFactory s3ClientFactory(@Value("${aws.key}") final String accessKey,
			@Value("${aws.privateKey}") final String privateKey,
			@Value("${s3.offline.directory}") final String directory) throws IOException {
		s3ClientFactory = new S3ClientFactory();
		s3ClientFactory.setOnlineImplementation(getOnlineImplementation(accessKey, privateKey));
		s3ClientFactory.setOfflineImplementation(getOfflineImplementation(directory));
		return s3ClientFactory;
	}

	private S3Client getOnlineImplementation(final String accessKey, final String privateKey) {
		return new S3ClientImpl(new BasicAWSCredentials(accessKey, privateKey));
	}

	private S3Client getOfflineImplementation(final String directory) throws IOException {
		return new OfflineS3ClientImpl(directory);
	}

	@Bean
	@DependsOn("s3ClientFactory")
	public S3Client s3Client(@Value("${aws.key}") final String accessKey,
			@Value("${aws.privateKey}") final String privateKey,
			@Value("${s3.offline.directory}") final String directory,
			@Value("${offlineMode}") final boolean offlineMode) throws IOException {
		s3ClientFactory = new S3ClientFactory();
		s3ClientFactory.setOnlineImplementation(getOnlineImplementation(accessKey, privateKey));
		s3ClientFactory.setOfflineImplementation(getOfflineImplementation(directory));
		return s3ClientFactory.getClient(offlineMode);
	}

	public S3Client getS3Client(final boolean offlineMode) {
		return s3ClientFactory.getClient(offlineMode);
	}

	@Bean
	@DependsOn("s3ClientFactory")
	public S3ClientHelper s3ClientHelper(@Value("${aws.key}") final String accessKey,
			@Value("${aws.privateKey}") final String privateKey,
			@Value("${s3.offline.directory}") final String directory,
			@Value("${offlineMode}") final boolean offlineMode) throws IOException {
		return new S3ClientHelper(s3Client(accessKey, privateKey, directory, offlineMode));
	}

	@Bean
	public SchemaFactory schemaFactory() {
		return new SchemaFactory();
	}

	@Bean
	public SimpleCacheManager cacheManager() {
		final SimpleCacheManager cacheManager = new SimpleCacheManager();
		cacheManager.setCaches(Arrays.asList(getCache("release-center-records"),
				getCache("global-roles"), getCache("code-system-roles")));
		return cacheManager;
	}

	private Cache getCache(final String name) {
		return new ConcurrentMapCache(name);
	}

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

	@Bean
	public StandardPasswordEncoder standardPasswordEncoder(@Value("${encryption.salt}") final String salt) {
		return new StandardPasswordEncoder(salt);
	}

	@Bean
	public Queue srsQueue(@Value("${srs.jms.job.queue}") final String queue) {
		return new ActiveMQQueue(queue);
	}

	@Bean
	public Queue buildStatusQueue(final String queue) {
		return new ActiveMQQueue(queue);
	}

	@Bean
	public ActiveMQTextMessage buildStatusTextMessage(@Value("${build.status.jms.job.queue}") final String queue) throws JMSException {
		final ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
		activeMQTextMessage.setJMSReplyTo(buildStatusQueue(queue));
		return activeMQTextMessage;
	}

	@Bean
	public MessageConverter jacksonJmsMessageConverter() {
		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		converter.setTargetType(MessageType.TEXT);
		converter.setTypeIdPropertyName("_type");
		return converter;
	}

	@Bean
	public MessagingHelper messagingHelper() {
		return new MessagingHelper();
	}

	@Bean
	public SimpleJmsListenerContainerFactory jmsListenerContainerFactory(@Value("${orchestration.jms.url}") final String brokerUrl,
			@Value("${orchestration.jms.username}") final String username, @Value("${orchestration.jms.password}") final String password) {
		final SimpleJmsListenerContainerFactory simpleJmsListenerContainerFactory =
				new SimpleJmsListenerContainerFactory();
		simpleJmsListenerContainerFactory.setConnectionFactory(new ActiveMQConnectionFactory(username, password, brokerUrl));
		return simpleJmsListenerContainerFactory;
	}

	@Bean
	public ActiveMQConnectionFactory jmsConnectionFactory(@Value("${orchestration.jms.url}") final String brokerUrl,
			@Value("${orchestration.jms.username}") final String username, @Value("${orchestration.jms.password}") final String password) {
		return new ActiveMQConnectionFactory(username, password, brokerUrl);
	}

	@Bean
	public ConnectionFactory connectionFactory(@Value("${orchestration.jms.url}") final String brokerUrl,
			@Value("${orchestration.jms.username}") final String username, @Value("${orchestration.jms.password}") final String password) {
		return new CachingConnectionFactory(jmsConnectionFactory(brokerUrl, username, password));
	}

	@Bean
	public JmsTemplate jmsTemplate(@Autowired ConnectionFactory connectionFactory) {
		return new JmsTemplate(connectionFactory);
	}
}
