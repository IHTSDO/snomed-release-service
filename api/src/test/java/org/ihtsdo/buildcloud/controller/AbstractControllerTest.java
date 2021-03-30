package org.ihtsdo.buildcloud.controller;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.buildcloud.config.DailyBuildResourceConfig;
import org.ihtsdo.buildcloud.config.HibernateTransactionManagerConfiguration;
import org.ihtsdo.buildcloud.config.JmsApiConfiguration;
import org.ihtsdo.buildcloud.config.LocalSessionFactoryBeanConfiguration;
import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.dao.*;
import org.ihtsdo.buildcloud.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.service.*;
import org.ihtsdo.buildcloud.service.build.readme.ReadmeGenerator;
import org.ihtsdo.buildcloud.service.build.transform.LegacyIdTransformationService;
import org.ihtsdo.buildcloud.service.build.transform.PesudoUUIDGenerator;
import org.ihtsdo.buildcloud.service.build.transform.TransformationService;
import org.ihtsdo.buildcloud.service.build.transform.UUIDGenerator;
import org.ihtsdo.buildcloud.service.identifier.client.IdServiceRestClient;
import org.ihtsdo.buildcloud.service.identifier.client.IdServiceRestClientOfflineDemoImpl;
import org.ihtsdo.buildcloud.service.postcondition.PostconditionManager;
import org.ihtsdo.buildcloud.service.precondition.PreconditionManager;
import org.ihtsdo.buildcloud.service.workbenchdatafix.ModuleResolverService;
import org.ihtsdo.buildcloud.service.worker.JmsConfiguration;
import org.ihtsdo.otf.dao.s3.OfflineS3ClientImpl;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.TestS3Client;
import org.ihtsdo.otf.dao.s3.helper.S3ClientHelper;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

@EnableConfigurationProperties
@PropertySource(value = "classpath:application.properties", encoding = "UTF-8")
@TestConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {BuildDAOImpl.class, ProductServiceImpl.class, BuildServiceImpl.class,
		PublishServiceImpl.class, ProductDAOImpl.class, OfflineS3ClientImpl.class, S3ClientHelper.class,
		ObjectMapper.class, BuildS3PathHelper.class, ProductInputFileDAOImpl.class, LocalSessionFactoryBeanConfiguration.class,
		HibernateTransactionManagerConfiguration.class, ExtensionConfigDAOImpl.class, ReleaseCenterDAOImpl.class,
		IdServiceRestClientOfflineDemoImpl.class, SchemaFactory.class, PreconditionManager.class, PostconditionManager.class,
		ReadmeGenerator.class, TransformationService.class, PesudoUUIDGenerator.class, ModuleResolverService.class,
		LegacyIdTransformationService.class, DailyBuildResourceConfig.class, TermServerServiceImpl.class, ReleaseCenterServiceImpl.class,
		ProductInputFileServiceImpl.class, HypermediaGenerator.class, PermissionService.class, PermissionServiceCache.class,
		ConcurrentMapCacheManager.class, ReleaseServiceImpl.class, JmsTemplate.class, JmsApiConfiguration.class})
@WebAppConfiguration
@ComponentScan(basePackages = "org.ihtsdo.buildcloud.controller")
public abstract class AbstractControllerTest {

	public static final MediaType APPLICATION_JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON.getType(),
			MediaType.APPLICATION_JSON.getSubtype(),
			Charset.forName("utf8")
		);

	public static final String ROOT_URL = "http://localhost";

	protected MockMvc mockMvc;

	@Autowired
	private JmsConfiguration jmsConfiguration;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private S3Client s3Client;

	@Autowired
	private UUIDGenerator uuidGenerator;

	@Autowired
	private IdServiceRestClient idRestClient;

	@Value("${buildBucketName}")
	private String buildBucketName;

	@Value("${publishedBucketName}")
	private String publishedBucketName;

	@Before
	public void setup() throws Exception {
		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setEncoding("UTF-8");
		filter.setForceEncoding(true);
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).addFilter(filter, "/*").build();
		Assert.assertNotNull(mockMvc);
		if (uuidGenerator instanceof PesudoUUIDGenerator) {
			((PesudoUUIDGenerator)uuidGenerator).reset();
		}
		if ( idRestClient instanceof IdServiceRestClientOfflineDemoImpl) {
			((IdServiceRestClientOfflineDemoImpl)idRestClient).reset();
		}
		if (s3Client instanceof TestS3Client) {
			final TestS3Client testS3Client = (TestS3Client) s3Client;
			testS3Client.freshBucketStore();
			testS3Client.createBucket(buildBucketName);
			testS3Client.createBucket(publishedBucketName);
		}
		
	}

	@After
	public void tearDown() {
		if (uuidGenerator instanceof PesudoUUIDGenerator) {
			((PesudoUUIDGenerator)uuidGenerator).reset();
		}
		if ( idRestClient instanceof IdServiceRestClientOfflineDemoImpl) {
			((IdServiceRestClientOfflineDemoImpl)idRestClient).reset();
		}
	}

}
