package org.ihtsdo.buildcloud.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.rcarz.jiraclient.Field;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import org.ihtsdo.buildcloud.core.dao.RVFFailureJiraAssociationDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.entity.RVFFailureJiraAssociation;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.jira.ImpersonatingJiraClientFactory;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

@Service
@Transactional
@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
public class RVFFailureJiraAssociationService {

	private static Logger logger = LoggerFactory.getLogger(RVFFailureJiraAssociationService.class);

	@Autowired
	private ImpersonatingJiraClientFactory jiraClientFactory;

	@Value("${jira.url}")
	private String jiraUrl;

	@Value("${jira.project}")
	private String project;

	@Value("${jira.issueType}")
	private String issueType;

	@Value("${jira.ticket.assignee}")
	private String assignee;

	@Value("${jira.ticket.priority}")
	private String priority;

	@Value("${jira.ticket.customField.product.release.date}")
	private String productReleaseDate;

	@Value("${jira.ticket.customField.reporting.entity}")
	private String reportingEntity;

	@Value("${jira.ticket.customField.reporting.entity.default.value}")
	private String reportingEntityDefaultValue;

	@Value("${jira.ticket.customField.reporting.stage}")
	private String reportingStage;

	@Value("${jira.ticket.customField.reporting.stage.default.value}")
	private String reportingStageDefaultValue;

	@Value("${jira.ticket.customField.snomedct.product}")
	private String snomedCtProduct;

	@Autowired
	private RVFFailureJiraAssociationDAO rvfFailureJiraAssociationDAO;

	@Autowired
	private BuildService buildService;

	@Autowired
	private ProductService productService;

	public List<RVFFailureJiraAssociation> findByBuildKey(String centerKey, String productKey, String buildKey) {
		return rvfFailureJiraAssociationDAO.findByBuildKey(centerKey, productKey, buildKey);
	}

	public Map<String, List<RVFFailureJiraAssociation>> createFailureJiraAssociations(String centerKey, String productKey, String buildKey, String[] assertionIds) throws BusinessServiceException, IOException, JiraException {
		if (assertionIds.length == 0) {
			throw new IllegalArgumentException("Assertion IDs must not be empty");
		}
		Product product = productService.find(centerKey, productKey, false);
		Build build = buildService.find(centerKey, productKey, buildKey, true, false, true, null);
		if (!StringUtils.hasLength(build.getRvfURL())) {
			throw new BusinessServiceException("RVF URL not found");
		}

		ValidationReport report = getRVFReport(build.getRvfURL());
		if (!report.isComplete()) {
			throw new BusinessServiceException("RVF report must be completed");
		}

		// check duplicate
		List<String> validAssertionIds = new ArrayList<>();
		List<RVFFailureJiraAssociation> dupplicatedAssocs = new ArrayList<>();
		List<RVFFailureJiraAssociation> existingAssocs = rvfFailureJiraAssociationDAO.findByEffectiveTime(centerKey, productKey, build.getConfiguration().getEffectiveTime());
		if (!existingAssocs.isEmpty()) {
			for (String assertionId : assertionIds) {
				RVFFailureJiraAssociation existingAssoc = existingAssocs.stream().filter(item -> item.getAssertionId().equals(assertionId)).findFirst().orElse(null);
				if (existingAssoc == null) {
					validAssertionIds.add(assertionId);
				} else {
					dupplicatedAssocs.add(existingAssoc);
				}
			}
		} else {
			validAssertionIds = Arrays.asList(assertionIds);
		}

		List<RVFFailureJiraAssociation> associations = new ArrayList<>();
		List<ValidationReport.RvfValidationResult.TestResult.TestRunItem> assertionsFailedAndWarning = getAllAssertions(report);
		for (String assertionId : validAssertionIds) {
			ValidationReport.RvfValidationResult.TestResult.TestRunItem found = assertionsFailedAndWarning.stream().filter(item -> item.getAssertionUuid() != null && item.getAssertionUuid().equals(assertionId)).findAny().orElse(null);
			if (found != null) {
				Issue jiraIssue = createJiraIssue(product.getReleaseCenter(), generateSummary(product, build, found), generateDescription(build, found), build.getConfiguration().getEffectiveTimeFormatted());
				Issue.NewAttachment[] attachments = new Issue.NewAttachment[1];
				attachments[0] = new Issue.NewAttachment(found.getAssertionUuid() + ".json", getPrettyString(found.toString()).getBytes());
				jiraIssue.addAttachments(attachments);

				final RVFFailureJiraAssociation association = new RVFFailureJiraAssociation(product.getReleaseCenter(), product, buildKey, build.getConfiguration().getEffectiveTime(), found.getAssertionUuid(), jiraUrl + "browse/" + jiraIssue.getKey());
				rvfFailureJiraAssociationDAO.save(association);
				associations.add(association);
			} else {
				logger.error("No failure found for the assertion {} for the build {}", assertionId, build.getId());
			}
		}
		Map<String, List<RVFFailureJiraAssociation>> result = new HashMap<>();
		result.put("newlyCreatedRVFFailureJiraAssociations", associations);
		result.put("duplicatedRVFFailureJiraAssociations", dupplicatedAssocs);

		return result;
	}

	private String generateSummary(Product product, Build build, ValidationReport.RvfValidationResult.TestResult.TestRunItem testRunItem) {
		return product.getName() + ", " + build.getConfiguration().getEffectiveTimeFormatted() + ", " + testRunItem.getTestType().replace("DROOL_RULES", "DROOLS") + ", "+ testRunItem.getAssertionUuid() + ", " + build.getId();
	}

	private String generateDescription(Build build, ValidationReport.RvfValidationResult.TestResult.TestRunItem testRunItem) {
		String result = testRunItem.getAssertionText() + "\n"
				+ "Total number of failures: " + testRunItem.getFailureCount() + "\n"
				+ "Report URL: " + "[" + build.getRvfURL() + "|" + build.getRvfURL() + "]" + "\n";
		List<ValidationReport.RvfValidationResult.TestResult.TestRunItem.FailureDetail> firstNInstances = getFirstNInstances(testRunItem.getFirstNInstances(), 10);
		result += "First " + firstNInstances.size() + " failures: \n";
		for (ValidationReport.RvfValidationResult.TestResult.TestRunItem.FailureDetail failureDetail: firstNInstances) {
			result += "* " + failureDetail.toString() + "\n";
		}

		return result;
	}

	private String getPrettyString(String input) {
		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(input);
		return gson.toJson(je);
	}

	private List<ValidationReport.RvfValidationResult.TestResult.TestRunItem.FailureDetail> getFirstNInstances(List<ValidationReport.RvfValidationResult.TestResult.TestRunItem.FailureDetail> instances, int numberOfItem) {
		if (numberOfItem < 0) {
			return instances;
		}

		int firstNCount = Math.min(numberOfItem, instances.size());

		return instances.subList(0, firstNCount);
	}

	private List<ValidationReport.RvfValidationResult.TestResult.TestRunItem> getAllAssertions(ValidationReport report) {
		List<ValidationReport.RvfValidationResult.TestResult.TestRunItem> assertionsFailedAndWarning = new ArrayList<>();
		if (report.getRvfValidationResult() != null && report.getRvfValidationResult().getTestResult() != null) {
			if (report.getRvfValidationResult().getTestResult().getAssertionsFailed() != null) {
				assertionsFailedAndWarning.addAll(report.getRvfValidationResult().getTestResult().getAssertionsFailed());
			}
			if (report.getRvfValidationResult().getTestResult().getAssertionsWarning() != null) {
				assertionsFailedAndWarning.addAll(report.getRvfValidationResult().getTestResult().getAssertionsWarning());
			}
		}
		return assertionsFailedAndWarning;
	}

	private Issue createJiraIssue(ReleaseCenter releaseCenter, String summary, String description, String releaseDate) throws BusinessServiceException {
		Issue jiraIssue;
		try {
			jiraIssue = getJiraClient().createIssue(project, issueType)
					.field(Field.SUMMARY, summary)
					.field(Field.DESCRIPTION, description)
					.field(Field.ASSIGNEE, getUsername())
					.execute();
			final Issue.FluentUpdate updateRequest = jiraIssue.update();
			updateRequest.field(Field.PRIORITY, priority);
			updateRequest.field(productReleaseDate, releaseDate);
			updateRequest.field(reportingEntity, Arrays.asList(reportingEntityDefaultValue));
			updateRequest.field(reportingStage, Arrays.asList(reportingStageDefaultValue));
			if (StringUtils.hasLength(assignee)) {
				updateRequest.field(Field.ASSIGNEE, assignee);
			}
			if (StringUtils.hasLength(releaseCenter.getSnomedCtProduct())) {
				updateRequest.field(snomedCtProduct, Arrays.asList(releaseCenter.getSnomedCtProduct().trim()));
			}
			updateRequest.execute();
		} catch (JiraException e) {
			logger.error(e.getMessage());
			throw new BusinessServiceException("Failed to create Jira task. Error: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
		}

		return jiraIssue;
	}

	private ValidationReport getRVFReport(String url) throws IOException {
		RestTemplate rvfRestTemplate = new RestTemplate();
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().failOnUnknownProperties(false).build();
		String validationReportString = rvfRestTemplate.getForObject(url, String.class);
		if (StringUtils.hasLength(validationReportString)) {
			validationReportString = validationReportString.replace("\"TestResult\"", "\"testResult\"");
		}

		return objectMapper.readValue(validationReportString, ValidationReport.class);
	}

	private JiraClient getJiraClient() {
		return jiraClientFactory.getImpersonatingInstance(getUsername());
	}

	private String getUsername() {
		return SecurityUtil.getUsername();
	}

	private static final class ValidationReport {

		public static final String COMPLETE = "COMPLETE";

		private String status;

		private RvfValidationResult rvfValidationResult;

		public boolean isComplete() {
			return COMPLETE.equals(status);
		}

		public String getStatus() {
			return status;
		}

		public RvfValidationResult getRvfValidationResult() {
			return rvfValidationResult;
		}

		private static final class RvfValidationResult {

			private TestResult testResult;

			public TestResult getTestResult() {
				return testResult;
			}

			private static final class TestResult {
				private List<TestRunItem> assertionsFailed;

				private List<TestRunItem> assertionsWarning;

				public List<TestRunItem> getAssertionsFailed() {
					return assertionsFailed;
				}

				public List<TestRunItem> getAssertionsWarning() {
					return assertionsWarning;
				}

				private static final class TestRunItem {
					private String testType;
					private String assertionUuid;
					private String assertionText;
					private String severity;
					private Long failureCount;
					private String failureMessage;
					private List<FailureDetail> firstNInstances;

					public String getTestType() {
						return testType;
					}

					public String getAssertionUuid() {
						return assertionUuid;
					}

					public String getAssertionText() {
						return assertionText;
					}

					public String getSeverity() {
						return severity;
					}

					public Long getFailureCount() {
						return failureCount;
					}

					public String getFailureMessage() {
						return failureMessage;
					}

					public List<FailureDetail> getFirstNInstances() {
						return firstNInstances;
					}

					@Override
					public String toString() {
						return "{" +
								"\"testType\": \"" + testType + '\"' +
								", \"assertionUuid\": \"" + assertionUuid + '\"' +
								", \"assertionText\": \"" + assertionText + '\"' +
								", \"severity\": " + (severity != null ? "\"" + severity + "\"" : null) +
								", \"failureCount\": " + failureCount +
								", \"failureMessage\": " + (failureMessage != null ? "\"" + failureMessage + "\"" : null) +
								", \"firstNInstances\": " + firstNInstances +
								'}';
					}

					private static final class FailureDetail {
						private String conceptId;
						private String conceptFsn;
						private String detail;
						private String componentId;
						private String fullComponent;

						public String getConceptId() {
							return conceptId;
						}

						public String getConceptFsn() {
							return conceptFsn;
						}

						public String getDetail() {
							return detail;
						}

						public String getComponentId() {
							return componentId;
						}

						public String getFullComponent() {
							return fullComponent;
						}

						@Override
						public String toString() {
							return "{\n\t" +
									"\"conceptId\": " + (conceptId != null ? '\"' + conceptId + '\"' : null) + ",\n\t" +
									"\"conceptFsn\": " + (conceptFsn != null ? '\"' + conceptFsn + '\"' : null) + ",\n\t" +
									"\"detail\": " + (detail != null ? '\"' + detail + '\"' : null) + ",\n\t" +
									"\"componentId\": " + (componentId != null ? '\"' + componentId + '\"' : null) + ",\n\t" +
									"\"fullComponent\": " + (fullComponent != null ? '\"' + fullComponent + '\"' : null) + "\n" +
									"}";
						}
					}
				}
			}
		}
	}
}
