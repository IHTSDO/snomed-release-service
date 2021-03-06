package org.ihtsdo.buildcloud.core.service.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.BuildStatusTrackerDao;
import org.ihtsdo.buildcloud.core.entity.*;
import org.ihtsdo.buildcloud.core.service.*;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.inputfile.gather.GatherInputRequestPojo;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystem;
import org.ihtsdo.otf.rest.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.jms.JMSException;
import javax.jms.Queue;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import static org.ihtsdo.buildcloud.core.entity.Build.Status.*;

@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
@Service
@Transactional
public class ReleaseBuildManager {

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private ProductInputFileService productInputFileService;

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

	private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseBuildManager.class);

	public Build createBuild(String releaseCenter, String productKey, GatherInputRequestPojo gatherInputRequestPojo, String currentUser) throws BusinessServiceException {
		// check if there is an existing build in progress
		throwExceptionIfBuildIsInProgressForProduct(productKey, releaseCenter);

		Product product = productService.find(releaseCenter, productKey, false);
		if (product == null) {
			LOGGER.error("Could not find product {} in release center {}", productKey, releaseCenter);
			throw new BusinessServiceRuntimeException("Could not find product " + productKey + " in release center " + releaseCenter);
		}

		validateBuildRequest(gatherInputRequestPojo, product);

		validateProductConfiguration(product);

		findManifestFileOrThrow(releaseCenter, productKey);

		//Create new build
		Integer maxFailureExport = gatherInputRequestPojo.getMaxFailuresExport() != null ? gatherInputRequestPojo.getMaxFailuresExport() : 100;
		QATestConfig.CharacteristicType mrcmValidationForm = gatherInputRequestPojo.getMrcmValidationForm() != null ? gatherInputRequestPojo.getMrcmValidationForm() : QATestConfig.CharacteristicType.stated;
		String branchPath = gatherInputRequestPojo.getBranchPath();
		String exportType = gatherInputRequestPojo.getExportCategory() != null ? gatherInputRequestPojo.getExportCategory().name() : null;
		String buildName = gatherInputRequestPojo.getBuildName();
		Date effectiveTime;
		try {
			effectiveTime = RF2Constants.DATE_FORMAT.parse(gatherInputRequestPojo.getEffectiveDate());
		} catch (ParseException e) {
			throw new BusinessServiceRuntimeException("Could not parse effectiveDate.");
		}
		Build build = buildService.createBuildFromProduct(releaseCenter, product.getBusinessKey(), buildName, currentUser, branchPath, exportType, maxFailureExport, mrcmValidationForm, effectiveTime);
		// add build status tracker
		if (build != null) {
			BuildStatusTracker tracker = new BuildStatusTracker();
			tracker.setProductKey(build.getProduct().getBusinessKey());
			tracker.setReleaseCenterKey(build.getProduct().getReleaseCenter().getBusinessKey());
			tracker.setBuildId(build.getId());
			statusTrackerDao.save(tracker);
		}
		return build;
	}

	public void queueBuild(final CreateReleasePackageBuildRequest createReleasePackageBuildRequest) throws BusinessServiceException {
		Build build = createReleasePackageBuildRequest.getBuild();
		if (build != null) {
			buildDAO.updateStatus(build, QUEUED);
			convertAndSend(createReleasePackageBuildRequest);
		} else {
			LOGGER.warn("Build can not be queued due to being null.");
		}
	}

	private void convertAndSend(final CreateReleasePackageBuildRequest build) throws BusinessServiceException {
		try {
			jmsTemplate.convertAndSend(srsQueue, objectMapper.writeValueAsString(build));
			LOGGER.info("Build {} has been sent to the {}.", build, srsQueue.getQueueName());
		} catch (JmsException | JsonProcessingException | JMSException e) {
			buildDAO.updateStatus(build.getBuild(), Build.Status.FAILED);
			LOGGER.error("Error occurred while trying to send the build to the srs queue: {}", srsQueue);
			throw new BusinessServiceException("Failed to send serialized build to the build queue. Build ID: " + build.getBuild().getId(), e);
		}
	}

	private void validateBuildRequest(GatherInputRequestPojo gatherInputRequestPojo, Product product) throws BusinessServiceException {
		if (StringUtils.isEmpty(gatherInputRequestPojo.getEffectiveDate())) {
			throw new BadRequestException("Effective Date must not be empty.");
		}
		if (gatherInputRequestPojo.isLoadTermServerData()) {
			if (StringUtils.isEmpty(gatherInputRequestPojo.getBranchPath())) {
				throw new BadRequestException("Branch path must not be empty.");
			}
			try {
				Branch branch = termServerService.getBranch(gatherInputRequestPojo.getBranchPath());
				if (branch == null) {
					throw new BadRequestException(String.format("Branch path %s does not exist.", gatherInputRequestPojo.getBranchPath()));
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
				throw new BusinessServiceException(String.format("Error occurred when getting branch %s", gatherInputRequestPojo.getBranchPath()), e);
			}
		}
	}

	private void validateProductConfiguration(Product product) throws BadRequestException {
		BuildConfiguration configuration = product.getBuildConfiguration();
		QATestConfig qaTestConfig = product.getQaTestConfig();

		final ReleaseCenter internationalReleaseCenter = new ReleaseCenter();
		internationalReleaseCenter.setShortName("International");

		final ReleaseCenter releaseCenter = product.getReleaseCenter();

		if (configuration != null) {
			if (StringUtils.isEmpty(configuration.getReadmeHeader())) {
				throw new BadRequestException("Readme Header must not be empty.");
			}

			if (StringUtils.isEmpty(configuration.getReadmeEndDate())) {
				throw new BadRequestException("Readme End Date must not be empty.");
			}

			if (!StringUtils.isEmpty(configuration.getPreviousPublishedPackage()) && !publishService.exists(releaseCenter, configuration.getPreviousPublishedPackage())) {
				throw new ResourceNotFoundException("Could not find previously published package: " + configuration.getPreviousPublishedPackage());
			}

			ExtensionConfig extensionConfig = configuration.getExtensionConfig();
			if (extensionConfig != null && !StringUtils.isEmpty(extensionConfig.getDependencyRelease())
					&& !publishService.exists(internationalReleaseCenter, extensionConfig.getDependencyRelease())) {
				throw new ResourceNotFoundException("Could not find dependency release package: " + extensionConfig.getDependencyRelease());
			}
		} else {
			throw new BadRequestException("Build configurations must not be null.");
		}

		if (qaTestConfig != null) {
			if (StringUtils.isEmpty(qaTestConfig.getAssertionGroupNames())) {
				throw new BadRequestException("RVF Assertion group name must not be empty.");
			}
			if (qaTestConfig.isEnableDrools() && StringUtils.isEmpty(qaTestConfig.getDroolsRulesGroupNames())) {
				throw new BadRequestException("Drool rule assertion group Name must not be empty.");
			}
			if (!StringUtils.isEmpty(qaTestConfig.getPreviousExtensionRelease()) && !publishService.exists(releaseCenter, qaTestConfig.getPreviousExtensionRelease())) {
				throw new ResourceNotFoundException("Could not find previous extension release package: " + qaTestConfig.getPreviousExtensionRelease());
			}
			if (!StringUtils.isEmpty(qaTestConfig.getExtensionDependencyRelease()) && !publishService.exists(internationalReleaseCenter, qaTestConfig.getExtensionDependencyRelease())) {
				throw new ResourceNotFoundException("Could not find extension dependency release package: " + qaTestConfig.getExtensionDependencyRelease());
			}
			if (!StringUtils.isEmpty(qaTestConfig.getPreviousInternationalRelease()) && !publishService.exists(internationalReleaseCenter, qaTestConfig.getPreviousInternationalRelease())) {
				throw new ResourceNotFoundException("Could not find previous international release package: " + qaTestConfig.getPreviousInternationalRelease());
			}
		}
	}

	private void findManifestFileOrThrow(String releaseCenter, String productKey) {
		String manifestFileName = productInputFileService.getManifestFileName(releaseCenter, productKey);
		if (StringUtils.isEmpty(manifestFileName)) {
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
