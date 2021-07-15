package org.ihtsdo.buildcloud.config;

import com.amazonaws.auth.BasicAWSCredentials;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import liquibase.integration.spring.SpringLiquibase;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.hibernate.SessionFactory;
import org.ihtsdo.otf.dao.s3.OfflineS3ClientImpl;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.S3ClientFactory;
import org.ihtsdo.otf.dao.s3.S3ClientImpl;
import org.ihtsdo.otf.dao.s3.helper.S3ClientHelper;
import org.ihtsdo.otf.jms.MessagingHelper;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cloud.aws.autoconfigure.context.ContextInstanceDataAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.SimpleJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import java.io.IOException;
import java.util.Arrays;

@SpringBootApplication(exclude = {
		ContextInstanceDataAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class}
)
@PropertySource(value = "classpath:application.properties", encoding = "UTF-8")
@EnableConfigurationProperties
@ComponentScan(basePackages = {"org.ihtsdo.buildcloud"})
@EnableJpaRepositories
@EnableJms
@EnableAsync
@EnableTransactionManagement
public abstract class Config extends BaseConfiguration {

	private static final String CHANGE_LOG_PATH = "classpath:org/ihtsdo/srs/db/changelog/db.changelog-master.xml";

	private S3ClientFactory s3ClientFactory;

	@Bean
	public ObjectMapper createObjectMapper() {
		return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Bean
	public DelegatingSecurityContextAsyncTaskExecutor securityContextAsyncTaskExecutor(
			@Value("${delegate.security.context.async.task.executor.thread.pool.size:10}") final int threadPoolSize) {
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
			@Value("${srs.jdbc.url}") final String url,
			@Value("${srs.jdbc.username}") final String username,
			@Value("${srs.jdbc.password}") final String password,
			@Value("${srs.hibernate.dialect}") final String dialect) {
		return getSessionFactory(driverClassName, url, username, password, dialect);
	}

	@Bean
	public HibernateTransactionManager transactionManager(@Value("${srs.jdbc.driverClassName}") final String driverClassName,
			@Value("${srs.jdbc.url}") final String url,
			@Value("${srs.jdbc.username}") final String username,
			@Value("${srs.jdbc.password}") final String password,
			@Value("${srs.hibernate.dialect}") final String dialect,
			@Autowired SessionFactory sessionFactory) {
		final HibernateTransactionManager transactionManager = new HibernateTransactionManager();
		transactionManager.setDataSource(getBasicDataSource(driverClassName, url, username, password));
		transactionManager.setSessionFactory(sessionFactory);
		return transactionManager;
	}

	@Bean
	public S3ClientFactory s3ClientFactory(@Value("${aws.key}") final String accessKey,
	                                       @Value("${aws.privateKey}") final String privateKey,
	                                       @Value("${srs.build.s3.offline.directory}") final String directory) throws IOException {
		s3ClientFactory = new S3ClientFactory();
		s3ClientFactory.setOnlineImplementation(new S3ClientImpl(new BasicAWSCredentials(accessKey, privateKey)));
		s3ClientFactory.setOfflineImplementation(new OfflineS3ClientImpl(directory));
		return s3ClientFactory;
	}

	@Bean
	public SpringLiquibase liquibase(@Value("${srs.jdbc.driverClassName}") final String driverClassName,
	                                 @Value("${srs.jdbc.url}") final String url, @Value("${srs.jdbc.username}") final String username,
	                                 @Value("${srs.jdbc.password}") final String password,
	                                 @Value("${srs.environment.shortname}") final String shortname) {
		final SpringLiquibase springLiquibase = new SpringLiquibase();
		springLiquibase.setDataSource(getBasicDataSource(driverClassName, url, username, password));
		springLiquibase.setChangeLog(CHANGE_LOG_PATH);
		springLiquibase.setContexts(shortname);
		return springLiquibase;
	}

	@Primary
	@Bean
	@DependsOn("s3ClientFactory")
	public S3Client s3Client(@Value("${srs.build.offlineMode}") final boolean offlineMode) {
		return s3ClientFactory.getClient(offlineMode);
	}

	@Bean
	@DependsOn("s3ClientFactory")
	public S3ClientHelper s3ClientHelper(@Value("${srs.build.offlineMode}") final boolean offlineMode) {
		return new S3ClientHelper(s3Client(offlineMode));
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
	public StandardPasswordEncoder standardPasswordEncoder(@Value("${encryption.salt}") final String salt) {
		return new StandardPasswordEncoder(salt);
	}

	@Bean
	public Queue srsQueue(@Value("${srs.jms.queue.prefix}.build-jobs") final String queue) {
		return new ActiveMQQueue(queue);
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
	public SimpleJmsListenerContainerFactory jmsListenerContainerFactory(@Autowired ConnectionFactory connectionFactory) {
		final SimpleJmsListenerContainerFactory simpleJmsListenerContainerFactory = new SimpleJmsListenerContainerFactory();
		simpleJmsListenerContainerFactory.setConnectionFactory(connectionFactory);
		return simpleJmsListenerContainerFactory;
	}

	@Bean
	public ActiveMQConnectionFactoryPrefetchCustomizer queuePrefetchCustomizer(@Value("${spring.activemq.queuePrefetch:1}") int queuePrefetch) {
		return new ActiveMQConnectionFactoryPrefetchCustomizer(queuePrefetch);
	}

	@Bean
	public JmsTemplate jmsTemplate(@Autowired ConnectionFactory connectionFactory) {
		return new JmsTemplate(connectionFactory);
	}

	@Bean
	public ActiveMQTextMessage buildStatusTextMessage(@Value("${srs.jms.queue.prefix}.build-job-status") final String queue) throws JMSException {
		return getActiveMQTextMessage(queue);
	}

	private ActiveMQTextMessage getActiveMQTextMessage(final String queue) throws JMSException {
		final ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
		activeMQTextMessage.setJMSReplyTo(new ActiveMQQueue(queue));
		return activeMQTextMessage;
	}
}
