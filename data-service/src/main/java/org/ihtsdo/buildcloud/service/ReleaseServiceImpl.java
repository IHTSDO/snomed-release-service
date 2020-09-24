package org.ihtsdo.buildcloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.ihtsdo.buildcloud.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.entity.*;
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
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Branch;
import org.ihtsdo.otf.rest.exception.*;
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
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReleaseServiceImpl implements ReleaseService {

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

    @Autowired
    private TermServerService termServerService;

    @Autowired
    private PublishService publishService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseServiceImpl.class);

    @Override
    public Build createBuild(String releaseCenter, String productKey, GatherInputRequestPojo gatherInputRequestPojo, String currentUser) throws BusinessServiceException {
        // Checking in-progress build for product
        if (concurrentReleaseBuildMap.containsKey(productKey)) {
            throw new EntityAlreadyExistsException("Product " + concurrentReleaseBuildMap.get(productKey) + " in release center " + releaseCenter + " has already a in-progress build");
        }

        validateBuildRequest(gatherInputRequestPojo);

        Product product = productService.find(releaseCenter, productKey);
        if (product == null) {
            LOGGER.error("Could not find product {} in release center {}", productKey, releaseCenter);
            throw new BusinessServiceRuntimeException("Could not find product " + productKey + " in release center " + releaseCenter);
        }

        validateProductConfiguration(product);

        findManifestFileOrThrow(releaseCenter, productKey);

        //Create new build
        Integer maxFailureExport = gatherInputRequestPojo.getMaxFailuresExport() != null ? gatherInputRequestPojo.getMaxFailuresExport() : 100;
        String branchPath = gatherInputRequestPojo.getBranchPath();
        String exportType = gatherInputRequestPojo.getExportCategory() != null ? gatherInputRequestPojo.getExportCategory().name() : null;
        String user = currentUser != null ? currentUser : User.ANONYMOUS_USER;
        String buildName = gatherInputRequestPojo.getBuildName();
        return buildService.createBuildFromProduct(releaseCenter, product.getBusinessKey(), buildName, user, branchPath, exportType, maxFailureExport);
    }

    @Override
    @Async("securityContextAsyncTaskExecutor")
    public void triggerBuildAsync(String releaseCenter, String productKey, Build build, GatherInputRequestPojo gatherInputRequestPojo, Authentication authentication) throws BusinessServiceException {
        Product product = build.getProduct();
        concurrentReleaseBuildMap.putIfAbsent(productKey, product.getName());


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

            Integer maxFailureExport = gatherInputRequestPojo.getMaxFailuresExport() != null ? gatherInputRequestPojo.getMaxFailuresExport() : 100;
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
            concurrentReleaseBuildMap.remove(product.getBusinessKey(), product.getName());
        }
    }

    private void validateProductConfiguration(Product product) throws BadRequestException {
        BuildConfiguration configuration = product.getBuildConfiguration();
        QATestConfig qaTestConfig = product.getQaTestConfig();
        final ReleaseCenter releaseCenter = product.getReleaseCenter();

        if (configuration != null) {
            if (!StringUtils.isEmpty(configuration.getPreviousPublishedPackage())) {
                try {
                    if (!publishService.exists(releaseCenter, configuration.getPreviousPublishedPackage())) {
                        throw new ResourceNotFoundException("Could not find previously published package: " + configuration.getPreviousPublishedPackage());
                    }
                } catch (ResourceNotFoundException e) {
                }
            }

            ExtensionConfig extensionConfig = configuration.getExtensionConfig();
            if (extensionConfig != null) {
                if (!StringUtils.isEmpty(extensionConfig.getDependencyRelease())) {
                    try {
                        if (!publishService.exists(releaseCenter, extensionConfig.getDependencyRelease())) {
                            throw new ResourceNotFoundException("Could not find dependency release package: " + extensionConfig.getDependencyRelease());
                        }
                    } catch (ResourceNotFoundException e) {
                    }
                }
            }
        }

        if (qaTestConfig != null) {
            if (StringUtils.isEmpty(qaTestConfig.getAssertionGroupNames())) {
                throw new BadRequestException("RVF Assertion group name must not be empty.");
            }
            if (qaTestConfig.isEnableDrools() && StringUtils.isEmpty(qaTestConfig.getDroolsRulesGroupNames())) {
                throw new BadRequestException("Drool rule assertion group Name must not be empty.");
            }
            if (!StringUtils.isEmpty(qaTestConfig.getPreviousExtensionRelease())) {
                try {
                    if (!publishService.exists(releaseCenter, qaTestConfig.getPreviousExtensionRelease())) {
                        throw new ResourceNotFoundException("Could not find previous extension release package: " + qaTestConfig.getPreviousExtensionRelease());
                    }
                } catch (ResourceNotFoundException e) {
                }
            }
            if (!StringUtils.isEmpty(qaTestConfig.getExtensionDependencyRelease())) {
                try {
                    if (!publishService.exists(releaseCenter, qaTestConfig.getExtensionDependencyRelease())) {
                        throw new ResourceNotFoundException("Could not find extension dependency release package: " + qaTestConfig.getExtensionDependencyRelease());
                    }
                } catch (ResourceNotFoundException e) {
                }
            }
            if (!StringUtils.isEmpty(qaTestConfig.getPreviousInternationalRelease())) {
                try {
                    if (!publishService.exists(releaseCenter, qaTestConfig.getPreviousInternationalRelease())) {
                        throw new ResourceNotFoundException("Could not find previous international release package: " + qaTestConfig.getPreviousInternationalRelease());
                    }
                } catch (ResourceNotFoundException e) {
                }
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

    private void saveInMemoryLogToS3(InMemoryLogAppender inMemoryLogAppender, Build build, Product product) throws BusinessServiceException {
        if (product != null || build != null) {
            List<LogOutputMessage> logOutputMessages = inMemoryLogAppender.getMessages();
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                LogOutputMessageList logOutputMessageList = new LogOutputMessageList(logOutputMessages);
                File tmpFile = File.createTempFile("tmp", ".json");
                FileUtils.write(tmpFile, objectMapper.writeValueAsString(logOutputMessageList));
                try {
                    s3Client.deleteObject(buildBucketName, s3PathHelper.getBuildFullLogJsonFromProduct(product));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (build != null) {
                    s3Client.putObject(buildBucketName, s3PathHelper.getBuildFullLogJsonFromBuild(build), tmpFile);
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
