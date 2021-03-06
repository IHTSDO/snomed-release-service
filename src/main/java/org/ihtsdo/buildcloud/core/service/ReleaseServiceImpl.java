package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.entity.QATestConfig;
import org.ihtsdo.buildcloud.core.service.inputfile.gather.GatherInputRequestPojo;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.ProductInputFileDAO;
import org.ihtsdo.buildcloud.core.entity.Build.Status;
import org.ihtsdo.buildcloud.core.service.inputfile.gather.InputGatherReport;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.FileProcessingReportDetail;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.ReportType;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.buildcloud.telemetry.client.TelemetryStream;
import org.ihtsdo.otf.rest.exception.BusinessServiceRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ReleaseServiceImpl implements ReleaseService {

	private static final String TRACKER_ID = "trackerId";

	private static final String PATTERN_ALL_FILES = "*.*";

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private ProductInputFileDAO productInputFileDAO;

	@Autowired
	private ProductInputFileService productInputFileService;

	@Autowired
	private BuildService buildService;

	private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseServiceImpl.class);

	private static final String ERROR_MSG_FORMAT = "Error encountered while running release build %s for product %s";

	@Override
	public void runReleaseBuild(String releaseCenter, String productKey, Build build, GatherInputRequestPojo gatherInputRequestPojo, Authentication authentication) {
		TelemetryStream.start(LOGGER, buildDAO.getTelemetryBuildLogFilePath(build));
		Product product = build.getProduct();

		try {
			MDC.put(TRACKER_ID, releaseCenter + "|" + product.getBusinessKey() + "|" + build.getId());
			LOGGER.info("Running release build for center/{}/product/{}/buildId/{}", releaseCenter, product.getBusinessKey(), build.getId());

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
				LOGGER.debug("GatherInputRequest {}", gatherInputRequestPojo);
				productInputFileService.deleteFilesByPattern(releaseCenter, product.getBusinessKey(), PATTERN_ALL_FILES);
				SourceFileProcessingReport sourceFileProcessingReport = productInputFileService.prepareInputFiles(releaseCenter, product.getBusinessKey(), true);
				if (sourceFileProcessingReport.getDetails().get(ReportType.ERROR) != null) {
					LOGGER.error("Error occurred when processing input files");
					List<FileProcessingReportDetail> errorDetails = sourceFileProcessingReport.getDetails().get(ReportType.ERROR);
					for (FileProcessingReportDetail errorDetail : errorDetails) {
						LOGGER.error("File: {} -> Error Details: {}", errorDetail.getFileName(), errorDetail.getMessage());
					}
				}
			}

			Integer maxFailureExport = gatherInputRequestPojo.getMaxFailuresExport() != null ? gatherInputRequestPojo.getMaxFailuresExport() : 100;
			QATestConfig.CharacteristicType mrcmValidationForm = gatherInputRequestPojo.getMrcmValidationForm() != null ? gatherInputRequestPojo.getMrcmValidationForm() : QATestConfig.CharacteristicType.stated;
			// trigger build
			LOGGER.info("Build {} is triggered for product {}", build.getId(), build.getProduct().getBusinessKey());
			buildService.triggerBuild(releaseCenter, product.getBusinessKey(), build.getId(), maxFailureExport, mrcmValidationForm, false);
		} catch (Exception e) {
			String msg = String.format(ERROR_MSG_FORMAT, build.getId(), build.getProduct().getBusinessKey());
			LOGGER.error(msg, e);
			buildDAO.updateStatus(build, Status.FAILED);
		} finally {
			MDC.remove(TRACKER_ID);
			TelemetryStream.finish(LOGGER);
		}
	}
}
