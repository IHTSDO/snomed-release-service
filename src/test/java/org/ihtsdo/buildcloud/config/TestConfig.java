package org.ihtsdo.buildcloud.config;

import org.ihtsdo.buildcloud.telemetry.server.TestBroker;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import javax.jms.JMSException;
import javax.jms.Session;

@PropertySource("classpath:application.properties")
@PropertySource(value = "classpath:application-test.properties", encoding = "UTF-8")
@EnableConfigurationProperties
@TestConfiguration
@Import(TelemetryConfig.class)
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

