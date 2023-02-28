package org.ihtsdo.buildcloud;

import org.ihtsdo.buildcloud.config.Config;
import org.ihtsdo.buildcloud.telemetry.server.TestBroker;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.aws.autoconfigure.context.ContextInstanceDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;

import javax.jms.JMSException;
import javax.jms.Session;

@PropertySource("classpath:application.properties")
@PropertySource(value = "classpath:application-test.properties", encoding = "UTF-8")
@EnableConfigurationProperties
@TestConfiguration
@SpringBootApplication(exclude = {
		ContextInstanceDataAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class}
)
public class TestConfig extends Config {

	@Bean
	public TestBroker broker() throws JMSException {
		return new TestBroker();
	}

	@Bean
	@DependsOn("broker")
	public Session jmsSession() throws JMSException {
		return broker().getSession();
	}
}

