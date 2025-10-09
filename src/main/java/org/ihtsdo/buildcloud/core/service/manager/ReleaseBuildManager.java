package org.ihtsdo.buildcloud.core.service.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.BuildStatusTrackerDao;
import org.ihtsdo.buildcloud.core.entity.*;
import org.ihtsdo.buildcloud.core.service.*;
import org.ihtsdo.buildcloud.rest.pojo.BuildRequestPojo;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystem;
import org.ihtsdo.otf.rest.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.ihtsdo.buildcloud.core.entity.Build.Status.*;

@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
@Service
@Transactional
public class ReleaseBuildManager {

	public static final String EPOCH_TIME = "1970-01-01T00:00:00";

	private static final String ENV_LOCAL = "local";

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private InputFileService inputFileService;

	@Autowired
	private BuildService buildService;

	@Autowired
	private ProductService productService;

	@Autowired
	private TermServerService termServerService;

	@Autowired
	private PublishService publishService;

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private Queue srsQueue;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private BuildStatusTrackerDao statusTrackerDao;

	@Autowired
	private PermissionService permissionService;

	@Value("${srs.environment.shortname}")
	private String envShortname;

	@Value("${srs.build.offlineMode}")
	private boolean offlineMode;

	@Value("${srs.manager}")
	private boolean isSrsManager;

	@Value("${srs.empty-release-file}")
	private String emptyRf2Filename;

	private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseBuildManager.class);

	@PostConstruct
	public void initialise() {
		if (!offlineMode && isSrsManager) {
			try {
				Build build = new Build(EPOCH_TIME, null, null, Build.Status.UNKNOWN.name());
				CreateReleasePackageBuildRequest buildRequest = new CreateReleasePackageBuildRequest(build, null, null);
				convertAndSend(buildRequest);
			} catch (BusinessServiceException | IOException e) {
				LOGGER.error("Failed to send EMPTY request", e);
			}
		}
	}

	public Build createBuild(String releaseCenter, String productKey, BuildRequestPojo buildRequestPojo, String currentUser) throws BusinessServiceException {
		Product product = productService.find(releaseCenter, productKey, false);
		if (product == null) {
			LOGGER.error("Could not find product {} in release center {}", productKey, releaseCenter);
			throw new BusinessServiceRuntimeException("Could not find product " + productKey + " in release center " + releaseCenter);
		}

		validateBuildRequest(buildRequestPojo, product);

		validateProductConfiguration(product);

		findManifestFileOrThrow(releaseCenter, productKey);

		List<String> userRoles = null;
		if (!ENV_LOCAL.equals(envShortname)) {
			Map<String, Set<String>> rolesToCodeSystemMap = permissionService.getRolesForLoggedInUser();
			if (!rolesToCodeSystemMap.isEmpty() && StringUtils.hasLength(product.getReleaseCenter().getCodeSystem())) {
				userRoles = new ArrayList<>(rolesToCodeSystemMap.get(product.getReleaseCenter().getCodeSystem()));
			}
		}

		return buildService.createBuildFromProduct(releaseCenter, product.getBusinessKey(), buildRequestPojo, currentUser, userRoles);
	}

	public void queueBuild(final CreateReleasePackageBuildRequest buildRequest) throws BusinessServiceException, IOException {
		if (buildRequest != null) {
			buildDAO.updateStatus(buildRequest.getBuild(), QUEUED);
			convertAndSend(buildRequest);
		} else {
			LOGGER.warn("Build can not be queued due to being null.");
		}
	}

	private void convertAndSend(final CreateReleasePackageBuildRequest buildRequest) throws BusinessServiceException, IOException {
		try {
			jmsTemplate.convertAndSend(srsQueue, objectMapper.writeValueAsString(buildRequest));
			LOGGER.info("Build {} has been sent to the {}.", buildRequest, srsQueue.getQueueName());
		} catch (JmsException | JsonProcessingException | JMSException e) {
			LOGGER.error("Failed to send serialized build. Message: {}", e.getMessage());
			LOGGER.error("Error occurred while trying to send the build to the srs queue: {}", srsQueue);
			buildDAO.updateStatus(buildRequest.getBuild(), Build.Status.FAILED);
			throw new BusinessServiceException("Failed to send serialized build to the build queue. Build ID: " + buildRequest.getBuild().getId(), e);
		}
	}

	private void validateBuildRequest(BuildRequestPojo buildRequestPojo, Product product) throws BusinessServiceException {
		if (buildRequestPojo.isLoadTermServerData()) {
			if (!StringUtils.hasLength(buildRequestPojo.getBranchPath())) {
				throw new BadRequestException("Branch path must not be empty.");
			}
			try {
				Branch branch = termServerService.getBranch(buildRequestPojo.getBranchPath());
				if (branch == null) {
					throw new BadRequestException(String.format("Branch path %s does not exist.", buildRequestPojo.getBranchPath()));
				}
				List<CodeSystem> codeSystems = termServerService.getCodeSystems();
				CodeSystem codeSystem = codeSystems.stream()
						.filter(c -> c.getShortName().equals(product.getReleaseCenter().getCodeSystem()))
						.findAny()
						.orElse(null);
				if (codeSystem != null && !branch.getPath().startsWith(codeSystem.getBranchPath())) {
					throw new BadRequestException(String.format("The branch path must be resided within the same code system branch %s", codeSystem.getBranchPath()));
				}
			} catch (RestClientException e) {
				throw new BusinessServiceException(String.format("Error occurred when getting branch %s", buildRequestPojo.getBranchPath()), e);
			}
		}
	}

	private void validateProductConfiguration(Product product) throws BusinessServiceException {
		BuildConfiguration configuration = product.getBuildConfiguration();
		QATestConfig qaTestConfig = product.getQaTestConfig();

		final ReleaseCenter internationalReleaseCenter = new ReleaseCenter();
		internationalReleaseCenter.setShortName("International");

		final ReleaseCenter releaseCenter = product.getReleaseCenter();

		if (configuration != null) {
			if (!StringUtils.hasLength(configuration.getReadmeHeader())) {
				throw new BadRequestException("Readme Header must not be empty.");
			}

			if (!StringUtils.hasLength(configuration.getReadmeEndDate())) {
				throw new BadRequestException("Readme End Date must not be empty.");
			}

			if (StringUtils.hasLength(configuration.getPreviousPublishedPackage()) && !emptyRf2Filename.equals(configuration.getPreviousPublishedPackage()) && !publishService.isReleaseFileExistInMSC(configuration.getPreviousPublishedPackage()) && !publishService.exists(releaseCenter, configuration.getPreviousPublishedPackage())) {
				throw new ResourceNotFoundException("Could not find previously published package: " + configuration.getPreviousPublishedPackage());
			}

			ExtensionConfig extensionConfig = configuration.getExtensionConfig();
			if (extensionConfig != null && StringUtils.hasLength(extensionConfig.getDependencyRelease())
					&& !publishService.exists(internationalReleaseCenter, extensionConfig.getDependencyRelease())) {
				throw new ResourceNotFoundException("Could not find dependency release package: " + extensionConfig.getDependencyRelease());
			}

			if (configuration.isDailyBuild()) {
				List<CodeSystem> codeSystems = termServerService.getCodeSystems();
				CodeSystem codeSystem = codeSystems.stream().filter(cs -> cs.getShortName().equalsIgnoreCase(product.getReleaseCenter().getCodeSystem())).findAny().orElse(null);
				if (codeSystem != null) {
					if (configuration.getExtensionConfig() != null && StringUtils.hasLength(configuration.getExtensionConfig().getDependencyRelease())
						&& !configuration.getExtensionConfig().getDependencyRelease().contains(codeSystem.getDependantVersionEffectiveTime().toString())) {
						throw new BusinessServiceException("The dependency release package is out of sync with code system. Could you please upgrade the dependant version.");
					}
				}
			}
		} else {
			throw new BadRequestException("Build configurations must not be null.");
		}

		if (qaTestConfig != null && (qaTestConfig.isEnableDrools() && !StringUtils.hasLength(qaTestConfig.getDroolsRulesGroupNames()))) {
				throw new BadRequestException("Drool rule assertion group Name must not be empty.");

		}
	}

	private void findManifestFileOrThrow(String releaseCenter, String productKey) {
		String manifestFileName = inputFileService.getManifestFileName(releaseCenter, productKey);
		if (!StringUtils.hasLength(manifestFileName)) {
			throw new ResourceNotFoundException(String.format("No manifest file found for product %s", productKey));
		}
	}

	public final void throwExceptionIfBuildIsInProgressForProduct(final String productKey, final String releaseCenter)
			throws EntityAlreadyExistsException {
		List<BuildStatusTracker> statusTrackers = statusTrackerDao.findByProductAndStatus(productKey, QUEUED.name(), BEFORE_TRIGGER.name(), BUILDING.name());
		if (statusTrackers != null && !statusTrackers.isEmpty()) {
			throw new EntityAlreadyExistsException(String.format("Product %s in release center %s already has an in-progress build %s",
					productKey, releaseCenter, statusTrackers.get(0).getBuildId()));
		}
	}
}
