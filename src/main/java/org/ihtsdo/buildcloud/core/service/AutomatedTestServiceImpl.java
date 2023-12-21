package org.ihtsdo.buildcloud.core.service;

import com.github.difflib.text.DiffRowGenerator;
import org.apache.commons.collections.CollectionUtils;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.Build.Status;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.core.service.build.compare.*;
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
import org.springframework.util.StringUtils;

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

	private final String SPACE_OF_FOUR = "    ";

	private Status[] BUILD_FINAL_STATES = { 	Status.FAILED_INPUT_GATHER_REPORT_VALIDATION,
											Status.FAILED_INPUT_PREPARE_REPORT_VALIDATION,
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
	private PublishService publishService;

	@Autowired
	private ReleaseCenterService releaseCenterService;

	@Autowired
	private BuildComparisonManager buildComparisonManager;

	private final LinkedBlockingQueue<BuildComparisonQueue> buildComparisonBlockingQueue = new LinkedBlockingQueue<>();

	private final LinkedBlockingQueue<FileComparisonQueue> fileComparisonBlockingQueue = new LinkedBlockingQueue<>();

	private ExecutorService buildComparisonExecutorService = Executors.newFixedThreadPool(1);

	private ExecutorService fileComparisonExecutorService = Executors.newFixedThreadPool(1);

	@Override
	public List<BuildComparisonReport> getAllTestReports() {
		List<BuildComparisonReport> reports = new ArrayList<>();
		List<ReleaseCenter> centers = releaseCenterService.findAll();
		Set<FilterOption> filterOptions = EnumSet.noneOf(FilterOption.class);

		centers.forEach(center -> {
			Page<Product> productPage = productService.findAll(center.getBusinessKey(), filterOptions, PageRequest.of(0, 100), false);
			List<Product> products = productPage.getContent();
			products.forEach(product -> {
				final String releaseCenterKey = product.getReleaseCenter().getBusinessKey();
				final String productKey = product.getBusinessKey();
				List<String> paths = buildDAO.listBuildComparisonReportPaths(releaseCenterKey, productKey);
				paths.forEach(path -> {
					try {
						BuildComparisonReport buildComparisonReport = buildDAO.getBuildComparisonReport(releaseCenterKey, productKey, path.replace(".json", ""));
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
		try {
			return buildDAO.getBuildComparisonReport(releaseCenterKey, productKey, compareId);
		} catch (IOException e) {
			throw new ResourceNotFoundException("Unable to find report for key: %s", compareId);
		}
	}

	@Override
	public void deleteTestReport(String releaseCenterKey, String productKey, String compareId) throws BusinessServiceException {
		BuildComparisonReport report = getTestReport(releaseCenterKey, productKey, compareId);
		if (report.getStatus().equals(BuildComparisonReport.Status.PASSED.name()) ||
				report.getStatus().equals(BuildComparisonReport.Status.FAILED.name()) ||
				report.getStatus().equals(BuildComparisonReport.Status.FAILED_TO_COMPARE.name()) ||
				(System.currentTimeMillis() - report.getStartDate().getTime()) > maxPollPeriod) {
			buildDAO.deleteBuildComparisonReport(releaseCenterKey, productKey, compareId);
			return;
		}

		throw new BusinessServiceException("Failed to delete report file. Build comparison is running");
	}

	@Override
	@Async
	public void compareBuilds(String compareId, String releaseCenterKey, String productKey, String leftBuildId, String rightBuildId, String username) {
		BuildComparisonReport report = new BuildComparisonReport();
		report.setCompareId(compareId);
		report.setStartDate(new Date());
		report.setUsername(username);
		report.setCenterKey(releaseCenterKey);
		report.setProductKey(productKey);
		report.setLeftBuildId(leftBuildId);
		report.setRightBuildId(rightBuildId);
		report.setStatus(BuildComparisonReport.Status.QUEUED.name());

		try {
			buildDAO.saveBuildComparisonReport(releaseCenterKey, productKey, compareId, report);
			Build leftBuild = getBuild(releaseCenterKey, productKey, leftBuildId, false);
			Build rightBuild = getBuild(releaseCenterKey, productKey, rightBuildId, false);
			if (Arrays.stream(BUILD_FINAL_STATES).noneMatch(status -> status.equals(leftBuild.getStatus()))) {
				try {
					waitForBuildCompleted(leftBuild, report);
				} catch (BusinessServiceException e) {
					report.setStatus(BuildComparisonReport.Status.FAILED_TO_COMPARE.name());
					report.setMessage(String.format("Failed to compare for id %s. Error message: %s", compareId, e.getMessage()));
					buildDAO.saveBuildComparisonReport(leftBuild.getReleaseCenterKey(), leftBuild.getProductKey(), compareId, report);
					return;
				}
			}
			if (Arrays.stream(BUILD_FINAL_STATES).noneMatch(status -> status.equals(rightBuild.getStatus()))) {
				try {
					waitForBuildCompleted(rightBuild, report);
				} catch (BusinessServiceException e) {
					report.setStatus(BuildComparisonReport.Status.FAILED_TO_COMPARE.name());
					report.setMessage(String.format("Failed to compare for id %s. Error message: %s", compareId, e.getMessage()));
					buildDAO.saveBuildComparisonReport(leftBuild.getReleaseCenterKey(), leftBuild.getProductKey(), compareId, report);
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
	 public void compareFiles(Build leftBuild, Build rightBuild, String fileName, String compareId, boolean ignoreIdComparison) {
		FileDiffReport report = null;
		try {
			report = buildDAO.getFileComparisonReport(leftBuild.getReleaseCenterKey(), leftBuild.getProductKey(), compareId, fileName, ignoreIdComparison);
		} catch (Exception e) {
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

			fileComparisonBlockingQueue.put(new FileComparisonQueue(compareId, fileName, leftBuild, rightBuild, report, ignoreIdComparison));
			buildDAO.saveFileComparisonReport(leftBuild.getReleaseCenterKey(), leftBuild.getProductKey(), compareId, ignoreIdComparison, report);
			processFileComparisonJobs();
		} catch (InterruptedException | IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	@Override
	public FileDiffReport getFileDiffReport(String releaseCenterKey, String productKey, String compareId, String fileName, boolean ignoreIdComparison) {
		try {
			return buildDAO.getFileComparisonReport(releaseCenterKey, productKey, compareId, fileName, ignoreIdComparison);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		return null;
	}

	protected void processBuildComparisonJobs() {
		buildComparisonExecutorService.submit(() -> {
			BuildComparisonQueue automatePromoteProcess = null;
			try {
				automatePromoteProcess = buildComparisonBlockingQueue.take();


				LOGGER.info("Staring build comparison for Id: {}", automatePromoteProcess.getCompareId());
				BuildComparisonReport report = automatePromoteProcess.getReport();
				report.setStatus(BuildComparisonReport.Status.COMPARING.name());
				buildDAO.saveBuildComparisonReport(automatePromoteProcess.getLeftBuild().getReleaseCenterKey(), automatePromoteProcess.getLeftBuild().getProductKey(), automatePromoteProcess.getCompareId(), report);

				Build leftBuild = automatePromoteProcess.getLeftBuild();
				Build rightBuild = automatePromoteProcess.getRightBuild();
				Build leftLatestBuild = getBuild(leftBuild.getReleaseCenterKey(), leftBuild.getProductKey(), leftBuild.getId(), true);
				Build rightLatestBuild = getBuild(rightBuild.getReleaseCenterKey(), rightBuild.getProductKey(), rightBuild.getId(), true);

				List<HighLevelComparisonReport> highLevelComparisonReports = buildComparisonManager.runBuildComparisons(leftLatestBuild, rightLatestBuild);
				boolean isFailed = highLevelComparisonReports.stream().anyMatch(c -> c.getResult().equals(HighLevelComparisonReport.State.FAILED));
				report.setStatus(isFailed ? BuildComparisonReport.Status.FAILED.name() : BuildComparisonReport.Status.PASSED.name());
				report.setReports(highLevelComparisonReports);

				buildDAO.saveBuildComparisonReport(leftBuild.getReleaseCenterKey(), leftBuild.getProductKey(), automatePromoteProcess.getCompareId(), report);
				LOGGER.info("Completed build comparison for Id: {}", automatePromoteProcess.getCompareId());
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
				if (automatePromoteProcess != null) {
					BuildComparisonReport report = automatePromoteProcess.getReport();
					report.setStatus(BuildComparisonReport.Status.FAILED_TO_COMPARE.name());
					report.setMessage(String.format("Failed to compare for id %s. Error message: %s", automatePromoteProcess.getCompareId(), e.getMessage()));
					try {
						buildDAO.saveBuildComparisonReport(automatePromoteProcess.getLeftBuild().getReleaseCenterKey(), automatePromoteProcess.getLeftBuild().getProductKey(), automatePromoteProcess.getCompareId(), report);
					} catch (IOException ioe) {
						LOGGER.error(ioe.getMessage(), ioe);
					}
				}
			}
		});
	}

	protected void processFileComparisonJobs() {
		fileComparisonExecutorService.submit(() -> {
			try {
				FileComparisonQueue automatePromoteProcess = fileComparisonBlockingQueue.take();
				Build leftBuild = automatePromoteProcess.getLeftBuild();
				Build rightBuild = automatePromoteProcess.getRightBuild();
				String fileName = automatePromoteProcess.getFileName();
				LOGGER.info("Staring file comparison for: {}", fileName);
				try (InputStream leftInputStream = buildDAO.getOutputFileInputStream(leftBuild, fileName);
					 InputStream rightInputStream = buildDAO.getOutputFileInputStream(rightBuild, fileName)) {
					List<String> leftList = new BufferedReader(new InputStreamReader(leftInputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
					List<String> rightList = new BufferedReader(new InputStreamReader(rightInputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.toList());

					DiffRowGenerator generator = DiffRowGenerator.create()
							.showInlineDiffs(false)
							.inlineDiffByWord(true)
							.reportLinesUnchanged(false)
							.build();
					List<com.github.difflib.text.DiffRow> diffRows = generator.generateDiffRows(leftList, rightList);
					leftList.clear();
					rightList.clear();

					extractResults(automatePromoteProcess, leftBuild, diffRows, automatePromoteProcess.isIgnoreIdComparison());
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

	private void extractResults(FileComparisonQueue automatePromoteProcess, Build leftBuild, List<com.github.difflib.text.DiffRow> diffRows, boolean ignoreIdComparison) throws IOException {
		if (diffRows.size() > 0) {
			diffRows = diffRows.stream().filter(r -> !com.github.difflib.text.DiffRow.Tag.EQUAL.equals(r.getTag()))
									.collect(Collectors.toList());
		} else {
			diffRows = Collections.EMPTY_LIST;
		}

		Map<String, String> leftIdToLineMap = new HashMap<>();
		Map<String, String> rightIdToLineMap = new HashMap<>();
		diffRows.forEach(row -> {
			if (StringUtils.hasLength(row.getOldLine())) {
				String[] arr = row.getOldLine().split(SPACE_OF_FOUR);
				if (StringUtils.hasLength(arr[0])) {
					leftIdToLineMap.put(arr[0], row.getOldLine());
				}
			}
			if (StringUtils.hasLength(row.getNewLine())) {
				String[] arr = row.getNewLine().split(SPACE_OF_FOUR);
				if (StringUtils.hasLength(arr[0])) {
					rightIdToLineMap.put(arr[0], row.getNewLine());
				}
			}
		});
		Set<String> leftIds = leftIdToLineMap.keySet();
		Set<String> rightIds = rightIdToLineMap.keySet();
		List<String> deleteIds = (ArrayList<String>) CollectionUtils.subtract(leftIds, rightIds);
		List<String> insertIds = (ArrayList<String>) CollectionUtils.subtract(rightIds, leftIds);
		List<String> changeIds = (ArrayList<String>) CollectionUtils.intersection(leftIds, rightIds);

		List<DiffRow> changeRows = new ArrayList<>();
		List<DiffRow> deleteRows = new ArrayList<>();
		List<DiffRow> insertRows = new ArrayList<>();

		findChangedRowsWithoutId(changeRows, deleteIds, insertIds, leftIdToLineMap, rightIdToLineMap, ignoreIdComparison);

		changeIds.forEach(id -> {
			if (!leftIdToLineMap.get(id).equals(rightIdToLineMap.get(id))) {
				changeRows.add(new DiffRow(leftIdToLineMap.get(id), rightIdToLineMap.get(id)));
			}
		});
		deleteIds.forEach(id -> deleteRows.add(new DiffRow(leftIdToLineMap.get(id),"")));
		insertIds.forEach(id -> insertRows.add(new DiffRow("", rightIdToLineMap.get(id))));

		FileDiffReport report = automatePromoteProcess.getReport();
		report.setStatus(FileDiffReport.Status.COMPLETED);
		report.setDeleteRows(deleteRows);
		report.setInsertRows(insertRows);
		report.setChangeRows(changeRows);
		buildDAO.saveFileComparisonReport(leftBuild.getReleaseCenterKey(), leftBuild.getProductKey(), automatePromoteProcess.getCompareId(), ignoreIdComparison, report);

		// clear temp list
		deleteIds.clear();
		insertIds.clear();
		changeIds.clear();
		leftIds.clear();
		rightIds.clear();
		leftIdToLineMap.clear();
		rightIdToLineMap.clear();
	}

	private void findChangedRowsWithoutId(List<DiffRow> changeRows, List<String> deleteIds, List<String> insertIds, Map<String, String> leftIdToLineMap, Map<String, String> rightIdToLineMap, boolean ignoreIdComparison) {
		Map<String, String> leftLineToIdMap = new HashMap<>();
		Map<String, String> rightLineToIdMap = new HashMap<>();
		for (String id : deleteIds) {
			leftLineToIdMap.put(leftIdToLineMap.get(id).replaceFirst(id, ""), id);
		}
		for (String id : insertIds) {
			rightLineToIdMap.put(rightIdToLineMap.get(id).replaceFirst(id, ""), id);
		}
		List<String> changeLines = (ArrayList<String>) CollectionUtils.intersection(leftLineToIdMap.keySet(), rightLineToIdMap.keySet());
		for (String changedLine : changeLines) {
			String leftId = leftLineToIdMap.get(changedLine);
			String rightId = rightLineToIdMap.get(changedLine);
			deleteIds.remove(leftId);
			insertIds.remove(rightId);
			if (!ignoreIdComparison) {
				changeRows.add(new DiffRow(leftIdToLineMap.get(leftId), rightIdToLineMap.get(rightId)));
			}
		}
	}

	private void waitForBuildCompleted(final Build build, BuildComparisonReport report) throws InterruptedException, BusinessServiceException, IOException {
		boolean isFinalState = false;
		int count = 0;
		while (!isFinalState) {
			Thread.sleep(pollPeriod);
			count += pollPeriod;

			Build latestBuild = getBuild(build.getReleaseCenterKey(), build.getProductKey(), build.getId(), false);
			if (!report.getStatus().equals(latestBuild.getStatus().name())) {
				report.setStatus(latestBuild.getStatus().name());
				buildDAO.saveBuildComparisonReport(build.getReleaseCenterKey(), build.getProductKey(), report.getCompareId(), report);
			}
			if (Arrays.stream(BUILD_FINAL_STATES).anyMatch(status -> status.equals(latestBuild.getStatus()))) {
				isFinalState = true;
			}

			if (count > maxPollPeriod) {
				throw new BusinessServiceException(String.format("Build with id %s did not complete within the allotted time (%s minutes).", build.getId(), maxPollPeriod/(60 * 1000)));
			}
		}
	}

	private Build getBuild(String releaseCenterKey, String productKey, String buildId, boolean includeRvfURL) {
		Build build;
		try {
			build = buildService.find(releaseCenterKey, productKey, buildId, false, false, includeRvfURL , null);
		} catch (ResourceNotFoundException e) {
			List<Build> publishedBuilds = publishService.findPublishedBuilds(releaseCenterKey, productKey, true);
			build = publishedBuilds.stream().filter(b -> b.getId().equals(buildId)).findAny().orElse(null);
			if (build == null) {
				throw e;
			}
		}

		return build;
	}

	private static class BuildComparisonQueue {
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

	private static class FileComparisonQueue {
		final private String compareId;
		final private String fileName;
		final private boolean ignoreIdComparison;
		private Build leftBuild;
		private Build rightBuild;
		private FileDiffReport report;

		FileComparisonQueue(String compareId, String fileName, Build leftBuild, Build rightBuild, FileDiffReport report, boolean ignoreIdComparison) {
			this.compareId = compareId;
			this.fileName = fileName;
			this.leftBuild = leftBuild;
			this.rightBuild = rightBuild;
			this.report = report;
			this.ignoreIdComparison = ignoreIdComparison;
		}

		public String getCompareId() {
			return compareId;
		}

		public boolean isIgnoreIdComparison() {
			return ignoreIdComparison;
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
