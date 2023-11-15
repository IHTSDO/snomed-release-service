package org.ihtsdo.buildcloud;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.ihtsdo.buildcloud.config.Config;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

@PropertySource(value = "classpath:application.properties", encoding = "UTF-8")
@PropertySource(value = "classpath:application-test.properties", encoding = "UTF-8")
@EnableConfigurationProperties
@EnableJpaRepositories
@TestConfiguration
@SpringBootApplication(exclude = {
		HibernateJpaAutoConfiguration.class}
)
public class TestConfig extends Config {
	private static final String ACTIVEMQ_IMAGE = "symptoma/activemq";
	private static final int ACTIVEMQ_PORT = 61616;

	@SuppressWarnings("rawtypes")
	@Container
	private static final GenericContainer activeMqContainer = new GenericContainer(ACTIVEMQ_IMAGE).withExposedPorts(ACTIVEMQ_PORT);
	static {
		System.setProperty("aws.region", "us-east-1");
		activeMqContainer.start();
	}

	@Bean
	public TestBroker broker() throws JMSException {
		String brokerUrlFormat = "tcp://%s:%d";
		String brokerUrl = String.format(brokerUrlFormat, activeMqContainer.getHost(), activeMqContainer.getFirstMappedPort());
		return new TestBroker(brokerUrl);
	}

	@Bean
	public ConnectionFactory connectionFactory() {
		String brokerUrlFormat = "tcp://%s:%d";
		String brokerUrl = String.format(brokerUrlFormat, activeMqContainer.getHost(), activeMqContainer.getFirstMappedPort());
		return new ActiveMQConnectionFactory(brokerUrl);
	}

	@Bean
	@DependsOn("broker")
	public Session jmsSession() throws JMSException {
		return broker().getSession();
	}
}

