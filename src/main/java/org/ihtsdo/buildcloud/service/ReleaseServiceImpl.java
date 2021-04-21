package org.ihtsdo.buildcloud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.ProductInputFileDAO;
import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Build.Status;
import org.ihtsdo.buildcloud.service.inputfile.gather.InputGatherReport;
import org.ihtsdo.buildcloud.service.inputfile.prepare.FileProcessingReportDetail;
import org.ihtsdo.buildcloud.service.inputfile.prepare.ReportType;
import org.ihtsdo.buildcloud.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.buildcloud.service.termserver.GatherInputRequestPojo;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.ihtsdo.otf.rest.exception.*;
import org.ihtsdo.buildcloud.telemetry.client.TelemetryStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.jms.JMSException;
import javax.jms.Queue;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.ihtsdo.buildcloud.service.build.RF2Constants.DATE_FORMAT;

@Service
public class ReleaseServiceImpl implements ReleaseService {

	private static final String TRACKER_ID = "trackerId";

	private static final String PATTERN_ALL_FILES = "*.*";

	private static final Map<String, String> concurrentReleaseBuildMap = new ConcurrentHashMap<>();

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private ProductInputFileDAO productInputFileDAO;

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

	private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseServiceImpl.class);

	@Override
	public Build createBuild(String releaseCenter, String productKey, GatherInputRequestPojo gatherInputRequestPojo, String currentUser) throws BusinessServiceException {
		// Checking in-progress build for product
		if (concurrentReleaseBuildMap.containsKey(productKey)) {
			throw new EntityAlreadyExistsException("Product " + concurrentReleaseBuildMap.get(productKey) + " in release center " + releaseCenter + " has already a in-progress build");
		}

		validateBuildRequest(gatherInputRequestPojo);

		Product product = productService.find(releaseCenter, productKey, false);
		if (product == null) {
			LOGGER.error("Could not find product {} in release center {}", productKey, releaseCenter);
			throw new BusinessServiceRuntimeException("Could not find product " + productKey + " in release center " + releaseCenter);
		}

		validateProductConfiguration(product);

		findManifestFileOrThrow(releaseCenter, productKey);

		//Create new build
		Integer maxFailureExport = gatherInputRequestPojo.getMaxFailuresExport() != null ? gatherInputRequestPojo.getMaxFailuresExport() : 100;
		QATestConfig.CharacteristicType mrcmValidationForm = gatherInputRequestPojo.getMrcmValidationForm() != null ? gatherInputRequestPojo.getMrcmValidationForm() : QATestConfig.CharacteristicType.stated;
		String branchPath = gatherInputRequestPojo.getBranchPath();
		String exportType = gatherInputRequestPojo.getExportCategory() != null ? gatherInputRequestPojo.getExportCategory().name() : null;
		String buildName = gatherInputRequestPojo.getBuildName();
		Date effectiveTime = null;
		try {
			effectiveTime = DATE_FORMAT.parse(gatherInputRequestPojo.getEffectiveDate());
		} catch (ParseException e) {
			throw new BusinessServiceRuntimeException("Could not parse effectiveDate.");
		}
		return buildService.createBuildFromProduct(releaseCenter, product.getBusinessKey(), buildName, currentUser, branchPath, exportType, maxFailureExport, mrcmValidationForm, effectiveTime);
	}

	@Override
	public CreateReleasePackageBuildRequest queueBuild(final CreateReleasePackageBuildRequest build) throws BusinessServiceException {
		buildDAO.updateStatus(build.getBuild(), Status.QUEUED);
		convertAndSend(build);
		return build;
	}

	private void convertAndSend(final CreateReleasePackageBuildRequest build) throws BusinessServiceException {
		try {
			jmsTemplate.convertAndSend(srsQueue, objectMapper.writeValueAsString(build));
			LOGGER.info("Build {} has been sent to the {}.", build, srsQueue.getQueueName());
		} catch (JmsException | JsonProcessingException | JMSException e) {
			buildDAO.updateStatus(build.getBuild(), Status.FAILED);
			LOGGER.info("Error occurred while trying to send the build to the srs queue: {}", srsQueue);
			throw new BusinessServiceException("Failed to send serialized build to the build queue. Build ID: " + build.getBuild().getId(), e);
		}
	}

	@Override
	@Async("securityContextAsyncTaskExecutor")
	public void triggerBuildAsync(String releaseCenter, String productKey, Build build, GatherInputRequestPojo gatherInputRequestPojo, Authentication authentication, String rootURL) throws BusinessServiceException {
		TelemetryStream.start(LOGGER, buildDAO.getTelemetryBuildLogFilePath(build));
		Product product = build.getProduct();
		concurrentReleaseBuildMap.putIfAbsent(productKey, product.getName());

		try {
			MDC.put(TRACKER_ID, releaseCenter + "|" + product.getBusinessKey() + "|" + build.getId());

			// Add build URL to log
			LOGGER.info("Build URL: " + rootURL + "/centers/{}/products/{}/builds/{}", releaseCenter, product.getBusinessKey(), build.getId());

			// clean up input-gather report and input-prepare report for product which were generated by previous build
			productInputFileDAO.deleteInputGatherReport(build.getProduct());
			productInputFileDAO.deleteInputPrepareReport(build.getProduct());

			//Gather all files in term server and externally maintain buckets if specified to source directories
			SecurityContext securityContext = new SecurityContextImpl();
			securityContext.setAuthentication(authentication);
			InputGatherReport inputGatherReport = productInputFileService.gatherSourceFiles(releaseCenter, product.getBusinessKey(), gatherInputRequestPojo, securityContext);
			if (inputGatherReport.getStatus().equals(InputGatherReport.Status.ERROR)) {
				LOGGER.error("Error occurred when gathering source files: ");
				for (String source : inputGatherReport.getDetails().keySet()) {
					InputGatherReport.Details details = inputGatherReport.getDetails().get(source);
					if (InputGatherReport.Status.ERROR.equals(details.getStatus())) {
						LOGGER.error("Source: {} -> Error Details: {}", source, details.getMessage());
						buildDAO.updateStatus(build, Status.FAILED);
						throw new BusinessServiceRuntimeException("Failed when gathering source files. Please check input gather report for details");
					}
				}
			}
			// After gathering all sources, start to transform and put them into input directories
			if (gatherInputRequestPojo.isLoadTermServerData() || gatherInputRequestPojo.isLoadExternalRefsetData()) {
				LOGGER.debug(gatherInputRequestPojo.toString());
				productInputFileService.deleteFilesByPattern(releaseCenter, product.getBusinessKey(), PATTERN_ALL_FILES);
				SourceFileProcessingReport sourceFileProcessingReport = productInputFileService.prepareInputFiles(releaseCenter, product.getBusinessKey(), true);
				if (sourceFileProcessingReport.getDetails().get(ReportType.ERROR) != null) {
					LOGGER.error("Error occurred when processing input files");
					List<FileProcessingReportDetail> errorDetails = sourceFileProcessingReport.getDetails().get(ReportType.ERROR);
					for (FileProcessingReportDetail errorDetail : errorDetails) {
						LOGGER.error("File: {} -> Error Details: {}", errorDetail.getFileName(), errorDetail.getMessage());
					}
//					if (!errorDetails.isEmpty()) {
//						buildDAO.updateStatus(build, Build.Status.FAILED);
//						throw new BusinessServiceRuntimeException("Failed when processing source files into input files. Please check input prepare report for details");
//					}
				}
			}

			Integer maxFailureExport = gatherInputRequestPojo.getMaxFailuresExport() != null ? gatherInputRequestPojo.getMaxFailuresExport() : 100;
			QATestConfig.CharacteristicType mrcmValidationForm = gatherInputRequestPojo.getMrcmValidationForm() != null ? gatherInputRequestPojo.getMrcmValidationForm() : QATestConfig.CharacteristicType.stated;
			// trigger build
			LOGGER.info("BUILD_INFO::/centers/{}/products/{}/builds/{}", releaseCenter, product.getBusinessKey(), build.getId());
			buildService.triggerBuild(releaseCenter, product.getBusinessKey(), build.getId(), maxFailureExport, mrcmValidationForm, false);
			LOGGER.info("Build {} is triggered {}", build.getProduct(), build.getId());
		} catch (IOException e) {
			LOGGER.error("Encounter error while creating package. Build process stopped.", e);
		} finally {
			MDC.remove(TRACKER_ID);
			concurrentReleaseBuildMap.remove(product.getBusinessKey(), product.getName());
			TelemetryStream.finish(LOGGER);
		}
	}

	@Override
	public void clearConcurrentCache(String releaseCenterKey, String productKey) {
		Product product = productService.find(releaseCenterKey, productKey, false);
		concurrentReleaseBuildMap.remove(product.getBusinessKey(), product.getName());
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

	private void validateBuildRequest(GatherInputRequestPojo gatherInputRequestPojo) throws BadRequestException {
		if (StringUtils.isEmpty(gatherInputRequestPojo.getEffectiveDate())) {
			throw new BadRequestException("Effective Date must not be empty.");
		}
		if (gatherInputRequestPojo.isLoadTermServerData()) {
			if (StringUtils.isEmpty(gatherInputRequestPojo.getBranchPath())) {
				throw new BadRequestException("Branch path must not be empty.");
			} else {
				try {
					Branch branch = termServerService.getBranch(gatherInputRequestPojo.getBranchPath());
					if (branch == null) {
						throw new BadRequestException("Branch path does not exist.");
					}
				} catch (RestClientException e) {
					LOGGER.error("Error occurred when getting branch {}. Error: {}", gatherInputRequestPojo.getBranchPath(), e.getMessage());
				}
			}
		}
	}

	private void findManifestFileOrThrow(String releaseCenter, String productKey) {
		String manifestFileName = productInputFileService.getManifestFileName(releaseCenter, productKey);
		if (StringUtils.isEmpty(manifestFileName)) {
			throw new ResourceNotFoundException("The manifest file does not exist.");
		}
	}
}
