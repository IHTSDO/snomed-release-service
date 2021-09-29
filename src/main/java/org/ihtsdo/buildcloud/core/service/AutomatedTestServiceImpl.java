package org.ihtsdo.buildcloud.core.service;

import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.Build.Status;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.core.service.build.compare.BuildComparisonManager;
import org.ihtsdo.buildcloud.core.service.build.compare.BuildComparisonReport;
import org.ihtsdo.buildcloud.core.service.build.compare.FileDiffReport;
import org.ihtsdo.buildcloud.core.service.build.compare.HighLevelComparisonReport;
import org.ihtsdo.buildcloud.core.service.helper.FilterOption;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

@Service
public class AutomatedTestServiceImpl implements AutomatedTestService {
	private static final Logger LOGGER = LoggerFactory.getLogger(AutomatedTestServiceImpl.class);

	private final int pollPeriod = 60 * 1000; /// 1 minute

	private final int maxPollPeriod = 4 * 60 * 60 * 1000; // 4 hours

	private Status[] BUILD_FINAL_STATE = { Status.FAILED_INPUT_PREPARE_REPORT_VALIDATION,
											Status.FAILED_PRE_CONDITIONS,
											Status.FAILED_POST_CONDITIONS,
											Status.CANCEL_REQUESTED,
											Status.CANCELLED,
											Status.FAILED,
											Status.RVF_FAILED,
											Status.RELEASE_COMPLETE,
											Status.RELEASE_COMPLETE_WITH_WARNINGS };

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private BuildService buildService;

	@Autowired
	private ProductService productService;

	@Autowired
	private ReleaseCenterService releaseCenterService;

	@Autowired
	private BuildComparisonManager buildComparisonManager;

	private final LinkedBlockingQueue<BuildComparisonQueue> buildComparisonBlockingQueue = new LinkedBlockingQueue<>(1);

	private final LinkedBlockingQueue<FileComparisonQueue> fileComparisonBlockingQueue = new LinkedBlockingQueue<>(1);

	private ExecutorService executorService = Executors.newFixedThreadPool(2);

	@Override
	public List<BuildComparisonReport> getAllTestReports() {
		List<BuildComparisonReport> reports = new ArrayList<>();
		List<ReleaseCenter> centers = releaseCenterService.findAll();
		Set<FilterOption> filterOptions = EnumSet.noneOf(FilterOption.class);

		centers.forEach(center -> {
			Page<Product> productPage = productService.findAll(center.getBusinessKey(), filterOptions, PageRequest.of(0, 100), false);
			List<Product> products = productPage.getContent();
			products.forEach(product -> {
				List<String> paths = buildDAO.listBuildComparisonReportPaths(product);
				paths.forEach(path -> {
					try {
						BuildComparisonReport buildComparisonReport = buildDAO.getBuildComparisonReport(product, path.replace(".json", ""));
						if (buildComparisonReport != null) {
							reports.add(buildComparisonReport);
						}
					} catch (IOException e) {
						LOGGER.error(e.getMessage(), e);
					}
				});
			});
		});

		return reports;
	}

	@Override
	public BuildComparisonReport getTestReport(String releaseCenterKey, String productKey, String compareId) {
		Product product = productService.find(releaseCenterKey, productKey, false);
		try {
			return buildDAO.getBuildComparisonReport(product, compareId);
		} catch (IOException e) {
			throw new ResourceNotFoundException(String.format("Unable to find report for key: %s"), compareId);
		}
	}

	@Override
	@Async
	public void compareBuilds(String compareId, Build leftBuild, Build rightBuild) {
		BuildComparisonReport report = new BuildComparisonReport();
		report.setCompareId(compareId);
		report.setStartDate(new Date());
		report.setCenterKey(leftBuild.getProduct().getReleaseCenter().getBusinessKey());
		report.setProductKey(leftBuild.getProduct().getBusinessKey());
		report.setLeftBuildId(leftBuild.getId());
		report.setRightBuildId(rightBuild.getId());
		report.setStatus(BuildComparisonReport.Status.RUNNING);
		try {
			buildDAO.saveBuildComparisonReport(leftBuild.getProduct(), compareId, report);
			if (!Arrays.stream(BUILD_FINAL_STATE).anyMatch(status -> status.equals(leftBuild.getStatus()))) {
				try {
					waitForBuildCompleted(leftBuild);
				} catch (BusinessServiceException e) {
					report.setStatus(BuildComparisonReport.Status.FAILED_TO_COMPARE);
					report.setMessage(String.format("Failed to compare for id %s. Error message: %s", compareId, e.getMessage()));
					buildDAO.saveBuildComparisonReport(leftBuild.getProduct(), compareId, report);
					return;
				}
			}
			if (!Arrays.stream(BUILD_FINAL_STATE).anyMatch(status -> status.equals(rightBuild.getStatus()))) {
				try {
					waitForBuildCompleted(rightBuild);
				} catch (BusinessServiceException e) {
					report.setStatus(BuildComparisonReport.Status.FAILED_TO_COMPARE);
					report.setMessage(String.format("Failed to compare for id %s. Error message: %s", compareId, e.getMessage()));
					buildDAO.saveBuildComparisonReport(leftBuild.getProduct(), compareId, report);
					return;
				}
			}
			buildComparisonBlockingQueue.put(new BuildComparisonQueue(compareId, leftBuild, rightBuild, report));
			processBuildComparisonJobs();
		} catch (InterruptedException | IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	@Override
	@Async
	 public void compareFiles(Build leftBuild, Build rightBuild, String fileName, String compareId) {
		FileDiffReport report = null;
		try {
			report = buildDAO.getFileComparisonReport(leftBuild.getProduct(), compareId, fileName);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
		if (report != null && (FileDiffReport.Status.RUNNING.equals(report.getStatus())
							|| FileDiffReport.Status.COMPLETED.equals(report.getStatus()))) {
			return;
		}
		try {
			report = new FileDiffReport();
			report.setStatus(FileDiffReport.Status.RUNNING);
			report.setFileName(fileName);
			report.setLeftBuildId(leftBuild.getId());
			report.setRightBuildId(rightBuild.getId());

			fileComparisonBlockingQueue.put(new FileComparisonQueue(compareId, fileName, leftBuild, rightBuild, report));
			buildDAO.saveFileComparisonReport(leftBuild.getProduct(), compareId, report);
			processFileComparisonJobs();
		} catch (InterruptedException | IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	@Override
	public FileDiffReport getFileDiffReport(String releaseCenterKey, String productKey, String compareId, String fileName) {
		Product product = productService.find(releaseCenterKey, productKey, false);
		try {
			return buildDAO.getFileComparisonReport(product, compareId, fileName);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return null;
	}

	protected void processBuildComparisonJobs() {
		executorService.submit(() -> {
			BuildComparisonQueue automatePromoteProcess = null;
			try {
				automatePromoteProcess = buildComparisonBlockingQueue.take();

				LOGGER.info("Staring build comparison for Id: {}", automatePromoteProcess.getCompareId());
				BuildComparisonReport report = automatePromoteProcess.getReport();

				Build leftBuild = automatePromoteProcess.getLeftBuild();
				Build rightBuild = automatePromoteProcess.getRightBuild();
				Build leftLatestBuild = buildDAO.find(leftBuild.getProduct(), leftBuild.getId(), false, false, true, null);
				Build rightLatestBuild = buildDAO.find(rightBuild.getProduct(), rightBuild.getId(), false, false, true, null);

				List<HighLevelComparisonReport> highLevelComparisonReports = buildComparisonManager.runBuildComparisons(leftLatestBuild, rightLatestBuild);
				boolean isFailed = highLevelComparisonReports.stream().anyMatch(c -> c.getResult().equals(HighLevelComparisonReport.State.FAILED));
				report.setStatus(isFailed ? BuildComparisonReport.Status.FAILED : BuildComparisonReport.Status.PASS);
				report.setReports(highLevelComparisonReports);

				buildDAO.saveBuildComparisonReport(leftBuild.getProduct(), automatePromoteProcess.getCompareId(), report);
				LOGGER.info("Completed build comparison for Id: {}", automatePromoteProcess.getCompareId());
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
				if (automatePromoteProcess != null) {
					BuildComparisonReport report = automatePromoteProcess.getReport();
					report.setStatus(BuildComparisonReport.Status.FAILED_TO_COMPARE);
					report.setMessage(String.format("Failed to compare for id %s. Error message: %s", automatePromoteProcess.getCompareId(), e.getMessage()));
					try {
						buildDAO.saveBuildComparisonReport(automatePromoteProcess.getLeftBuild().getProduct(), automatePromoteProcess.getCompareId(), report);
					} catch (IOException ioe) {
						LOGGER.error(ioe.getMessage(), ioe);
					}
				}
			}
		});
	}

	protected void processFileComparisonJobs() {
		executorService.submit(() -> {
			try {
				FileComparisonQueue automatePromoteProcess = fileComparisonBlockingQueue.take();
				Build leftBuild = automatePromoteProcess.getLeftBuild();
				Build rightBuild = automatePromoteProcess.getRightBuild();
				String fileName = automatePromoteProcess.getFileName();
				LOGGER.info("Staring file comparison for: {}", fileName);
				try (InputStream leftInputStream = buildDAO.getOutputFileInputStream(leftBuild, fileName);
					 InputStream rightInputStream = buildDAO.getOutputFileInputStream(rightBuild, fileName);) {
					List<String> leftList = new BufferedReader(new InputStreamReader(leftInputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
					List<String> rightList = new BufferedReader(new InputStreamReader(rightInputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.toList());

					DiffRowGenerator generator = DiffRowGenerator.create()
							.showInlineDiffs(false)
							.inlineDiffByWord(true)
							.reportLinesUnchanged(false)
							.build();
					List<DiffRow> diffRows = generator.generateDiffRows(leftList, rightList);
					if (diffRows.size() > 0) {
						diffRows = diffRows.stream().filter(diffRow -> !DiffRow.Tag.EQUAL.equals(diffRow.getTag())).collect(Collectors.toList());
					}
					leftList.clear();
					rightList.clear();
					FileDiffReport report = automatePromoteProcess.getReport();
					report.setStatus(FileDiffReport.Status.COMPLETED);
					report.setDiffRows(diffRows);
					buildDAO.saveFileComparisonReport(leftBuild.getProduct(), automatePromoteProcess.getCompareId(), report);
					LOGGER.info("Completed file comparison for: {}", fileName);
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
					throw new BusinessServiceException("Failed to compare file. Error message: " + e.getMessage());
				}
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		});
	}

	private void waitForBuildCompleted(final Build build) throws InterruptedException, BusinessServiceException {
		boolean isFinalState = false;
		int count = 0;
		final Product product = build.getProduct();
		while (!isFinalState) {
			Thread.sleep(pollPeriod);
			count += pollPeriod;

			Build latestBuild = buildService.find(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), build.getId(), false, false, false, null);
			if (Arrays.stream(BUILD_FINAL_STATE).anyMatch(status -> status.equals(latestBuild.getStatus()))) {
				isFinalState = true;
			}

			if (count > maxPollPeriod) {
				throw new BusinessServiceException(String.format("Build with id %s did not complete within the allotted time (%s minutes).", build.getId(), maxPollPeriod/(60 * 1000)));
			}
		}
	}

	private class BuildComparisonQueue {
		final private String compareId;
		private Build leftBuild;
		private Build rightBuild;
		private BuildComparisonReport report;

		BuildComparisonQueue(String compareId, Build leftBuild, Build rightBuild, BuildComparisonReport report) {
			this.compareId = compareId;
			this.leftBuild = leftBuild;
			this.rightBuild = rightBuild;
			this.report = report;
		}

		public String getCompareId() {
			return compareId;
		}

		public Build getLeftBuild() {
			return leftBuild;
		}

		public void setLeftBuild(Build leftBuild) {
			this.leftBuild = leftBuild;
		}

		public Build getRightBuild() {
			return rightBuild;
		}

		public void setRightBuild(Build rightBuild) {
			this.rightBuild = rightBuild;
		}

		public void setReport(BuildComparisonReport report) {
			this.report = report;
		}

		public BuildComparisonReport getReport() {
			return report;
		}
	}

	private class FileComparisonQueue {
		final private String compareId;
		final private String fileName;
		private Build leftBuild;
		private Build rightBuild;
		private FileDiffReport report;

		FileComparisonQueue(String compareId, String fileName, Build leftBuild, Build rightBuild, FileDiffReport report) {
			this.compareId = compareId;
			this.fileName = fileName;
			this.leftBuild = leftBuild;
			this.rightBuild = rightBuild;
			this.report = report;
		}

		public String getCompareId() {
			return compareId;
		}

		public String getFileName() {
			return fileName;
		}

		public Build getLeftBuild() {
			return leftBuild;
		}

		public void setLeftBuild(Build leftBuild) {
			this.leftBuild = leftBuild;
		}

		public Build getRightBuild() {
			return rightBuild;
		}

		public void setRightBuild(Build rightBuild) {
			this.rightBuild = rightBuild;
		}

		public void setReport(FileDiffReport report) {
			this.report = report;
		}

		public FileDiffReport getReport() {
			return report;
		}
	}
}
