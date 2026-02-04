package org.ihtsdo.buildcloud;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.ihtsdo.buildcloud.config.Config;
import org.ihtsdo.buildcloud.core.service.TermServerService;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient.ExportCategory;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystem;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystemVersion;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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

	/**
	 * Test stub for {@link TermServerService} to avoid hitting a real Snowstorm instance
	 * during tests that publish builds.
	 */
	@Bean
	@Primary
	public TermServerService termServerService() {
		return new TermServerService() {
			@Override
			public File export(String branchPath, String effectiveDate, Set<String> exportModuleIds, ExportCategory exportCategory) {
				// Not used in current tests
				return null;
			}

			@Override
			public List<CodeSystem> getCodeSystems() {
				// Return empty so callers simply skip code-system-based behaviour
				return Collections.emptyList();
			}

			@Override
			public List<CodeSystemVersion> getCodeSystemVersions(String shortName, boolean showFutureVersions, boolean showInternalReleases) {
				return Collections.emptyList();
			}

			@Override
			public Branch getBranch(String branchPath) throws RestClientException {
				// Minimal stub branch with no metadata
				Branch branch = new Branch();
				branch.setPath(branchPath);
				branch.setMetadata(Collections.emptyMap());
				return branch;
			}

			@Override
			public void updateCodeSystemVersionPackage(String codeSystemShortName, String effectiveDate, String releasePackage) {
				// No-op in tests
			}

			@Override
			public Set<String> getModulesForBranch(String branchPath) throws RestClientException {
				return Collections.emptySet();
			}
		};
	}
}

