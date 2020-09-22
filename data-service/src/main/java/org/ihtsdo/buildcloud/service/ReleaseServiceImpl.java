package org.ihtsdo.buildcloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.ihtsdo.buildcloud.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.helper.InMemoryLogAppender;
import org.ihtsdo.buildcloud.service.helper.LogOutputMessage;
import org.ihtsdo.buildcloud.service.helper.LogOutputMessageList;
import org.ihtsdo.buildcloud.service.helper.WebSocketLogAppender;
import org.ihtsdo.buildcloud.service.inputfile.gather.InputGatherReport;
import org.ihtsdo.buildcloud.service.inputfile.prepare.FileProcessingReportDetail;
import org.ihtsdo.buildcloud.service.inputfile.prepare.ReportType;
import org.ihtsdo.buildcloud.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.buildcloud.service.termserver.GatherInputRequestPojo;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.BusinessServiceRuntimeException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReleaseServiceImpl implements ReleaseService{

	private static final String TRACKER_ID = "trackerId";

	private static final String PATTERN_ALL_FILES = "*.*";

	private static Map concurrentReleaseBuildMap = new ConcurrentHashMap();

	@Autowired
	private ProductInputFileService productInputFileService;

	@Autowired
	private BuildService buildService;

	@Autowired
	private AuthenticationService authenticationService;

	@Autowired
	private S3Client s3Client;

	@Autowired
	private String buildBucketName;

	@Autowired
	private BuildS3PathHelper s3PathHelper;

	@Autowired
	private ProductService productService;

	private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseServiceImpl.class);


	@Override
	public void validateInProgressBuild(String releaseCenter, String productKey) throws BusinessServiceException {
		// Checking in-progress build for product
		if (concurrentReleaseBuildMap.containsKey(productKey)) {
			throw new EntityAlreadyExistsException("Product " + productKey + " in release center " + releaseCenter + " has already a in-progress build");
		}
	}

	@Override
	@Async("securityContextAsyncTaskExecutor")
	public void createAndTriggerBuild(String releaseCenter, String productKey, GatherInputRequestPojo gatherInputRequestPojo, Authentication authentication, String currentUser) throws BusinessServiceException {
		concurrentReleaseBuildMap.putIfAbsent(productKey, Boolean.TRUE);
		Product product = productService.find(releaseCenter, productKey);
		if (product == null) {
			LOGGER.error("Could not find product {} in release center {}", productKey, releaseCenter);
			throw new BusinessServiceRuntimeException("Could not find product " + productKey + " in release center " + releaseCenter);
		}

		//Create new build
		Integer maxFailureExport = gatherInputRequestPojo.getMaxFailuresExport() != null ? gatherInputRequestPojo.getMaxFailuresExport() : 100;
		String branchPath = gatherInputRequestPojo.getBranchPath();
		String exportType = gatherInputRequestPojo.getExportCategory() != null ?  gatherInputRequestPojo.getExportCategory().name() : null;
		String user = currentUser != null ? currentUser : User.ANONYMOUS_USER;
		String buildName = gatherInputRequestPojo.getBuildName();
		Build build = buildService.createBuildFromProduct(releaseCenter, product.getBusinessKey(), buildName, user, branchPath, exportType, maxFailureExport);

		String inMemoryLogTrackerId = Long.toString(new Date().getTime());
		InMemoryLogAppender inMemoryLogAppender = addInMemoryAppenderToLogger(inMemoryLogTrackerId);
		try {
			MDC.put(TRACKER_ID, releaseCenter + "|" + product.getBusinessKey() + "|" + build.getId());

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
						throw new BusinessServiceRuntimeException("Failed when gathering source files. Please check input gather report for details");
					}
				}
			}
			//After gathering all sources, start to transform and put them into input directories
			if (gatherInputRequestPojo.isLoadTermServerData() || gatherInputRequestPojo.isLoadExternalRefsetData()) {
				productInputFileService.deleteFilesByPattern(releaseCenter, product.getBusinessKey(), PATTERN_ALL_FILES);
				SourceFileProcessingReport sourceFileProcessingReport = productInputFileService.prepareInputFiles(releaseCenter, product.getBusinessKey(), true);
				if (sourceFileProcessingReport.getDetails().get(ReportType.ERROR) != null) {
					LOGGER.error("Error occurred when processing input files");
					List<FileProcessingReportDetail> errorDetails = sourceFileProcessingReport.getDetails().get(ReportType.ERROR);
					for (FileProcessingReportDetail errorDetail : errorDetails) {
						LOGGER.error("File: {} -> Error Details: {}", errorDetail.getFileName(), errorDetail.getMessage());
						throw new BusinessServiceRuntimeException("Failed when processing source files into input files. Please check input prepare report for details");
					}
				}
			}

			// trigger build
			LOGGER.info("BUILD_INFO::/centers/{}/products/{}/builds/{}", releaseCenter, product.getBusinessKey(), build.getId());
			buildService.triggerBuild(releaseCenter, product.getBusinessKey(), build.getId(), maxFailureExport);
			LOGGER.info("Build process ends", build.getStatus().name());
		} catch (IOException e) {
			LOGGER.error("Encounter error while creating package. Build process stopped.", e);
			throw new BusinessServiceException(e);
		} finally {
			MDC.remove(TRACKER_ID);
			saveInMemoryLogToS3(inMemoryLogAppender, build, product);
			removeInMemorySocketAppenderFromLogger(inMemoryLogTrackerId);
			concurrentReleaseBuildMap.remove(product.getBusinessKey(), Boolean.TRUE);
		}

	}

	private InMemoryLogAppender addInMemoryAppenderToLogger(String trackerId) {
		org.apache.log4j.Logger logger = LogManager.getLogger("org.ihtsdo");
		if (logger.getAppender("mem_" + trackerId) == null) {
			InMemoryLogAppender inMemoryLogAppender = new InMemoryLogAppender(trackerId);
			inMemoryLogAppender.setName("mem" + trackerId);
			logger.addAppender(inMemoryLogAppender);
			return inMemoryLogAppender;
		} else {
			return (InMemoryLogAppender) logger.getAppender("mem_" + trackerId);
		}
	}

	private void removeInMemorySocketAppenderFromLogger(String trackerId) {
		org.apache.log4j.Logger logger = LogManager.getLogger("org.ihtsdo");
		logger.removeAppender("mem_" + trackerId);
	}

	private void saveInMemoryLogToS3(InMemoryLogAppender inMemoryLogAppender, Build build, Product product) throws  BusinessServiceException {
		if(product != null || build !=null) {
			List<LogOutputMessage> logOutputMessages = inMemoryLogAppender.getMessages();
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				LogOutputMessageList logOutputMessageList = new LogOutputMessageList(logOutputMessages);
				File tmpFile = File.createTempFile("tmp",".json");
				FileUtils.write(tmpFile, objectMapper.writeValueAsString(logOutputMessageList));
				try {
					s3Client.deleteObject(buildBucketName, s3PathHelper.getBuildFullLogJsonFromProduct(product));
				} catch (Exception e) {
					e.printStackTrace();
				}
				if(build != null) {
					s3Client.putObject(buildBucketName,s3PathHelper.getBuildFullLogJsonFromBuild(build), tmpFile);
				} else {
					s3Client.putObject(buildBucketName, s3PathHelper.getBuildFullLogJsonFromProduct(product), tmpFile);
				}
			} catch (IOException e) {
				LOGGER.error("Failed to save log to S3", e);
				throw new BusinessServiceException(e);
			}
		}
	}
}
