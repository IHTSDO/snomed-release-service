package org.ihtsdo.buildcloud.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.rcarz.jiraclient.Field;
import net.sf.json.JSONObject;
import org.ihtsdo.buildcloud.core.dao.RVFFailureJiraAssociationDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.entity.RVFFailureJiraAssociation;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.jira.JiraCloudClient;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
public class RVFFailureJiraAssociationService {

	public static final String VALUE = "value";
	public static final String ACCOUNT_ID = "accountId";
	private static Logger logger = LoggerFactory.getLogger(RVFFailureJiraAssociationService.class);

	@Autowired
	private JiraCloudClient jiraCloudClient;

	@Value("${jira.cloud.project-key}")
	private String project;

	@Value("${jira.issueType}")
	private String issueType;

	@Value("${jira.cloud.assignee-accountid}")
	private String assignee;

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

	@Value("${jira.cloud.reporter-accountid}")
	private String reporter;

	@Autowired
	private RVFFailureJiraAssociationDAO rvfFailureJiraAssociationDAO;

	@Autowired
	private BuildService buildService;

	@Autowired
	private ProductService productService;

	private Map<String, String> jiraCustomFields;

	@Autowired
	public RVFFailureJiraAssociationService(@Value("${jira.ticket.customField.product.release.date}") final String productReleaseDate,
											@Value("${jira.ticket.customField.reporting.entity}") final String reportingEntity,
											@Value("${jira.ticket.customField.reporting.stage}") final String reportingStage,
											@Value("${jira.ticket.customField.snomedct.product}") final String snomedCtProduct) {
		jiraCustomFields = new HashMap<>();
		jiraCustomFields.put(productReleaseDate, "Product Release Date");
		jiraCustomFields.put(reportingEntity, "Reporting entity");
		jiraCustomFields.put(reportingStage, "Reporting stage");
		jiraCustomFields.put(snomedCtProduct, "SNOMED CT Product");
	}

	public List<RVFFailureJiraAssociation> findByBuildKey(String centerKey, String productKey, String buildKey) {
		return rvfFailureJiraAssociationDAO.findByBuildKey(centerKey, productKey, buildKey);
	}

	public Map<String, List<RVFFailureJiraAssociation>> createFailureJiraAssociations(String centerKey, String productKey, String buildKey, String[] assertionIds) throws BusinessServiceException, IOException {
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
			if (found == null) {
				logger.error("No failure found for the assertion {} for the build {}", assertionId, build.getId());
				continue;
			}
			String issueKey = createJiraIssue(generateSummary(product, build, found), generateDescription(build, found));
			final RVFFailureJiraAssociation association = new RVFFailureJiraAssociation(product.getReleaseCenter(), product, buildKey, build.getConfiguration().getEffectiveTime(), found.getAssertionUuid(), jiraCloudClient.getBaseUrl() + "browse/" + issueKey);
			rvfFailureJiraAssociationDAO.save(association);
			associations.add(association);

			// Add attachment and update JIRA custom fields
			jiraCloudClient.addAttachment(issueKey, found.getAssertionUuid() + ".json", getPrettyString(found.toString()).getBytes());
			updateJiraIssue(product, build.getConfiguration().getEffectiveTimeFormatted(), issueKey);
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
		StringBuilder result = new StringBuilder(testRunItem.getAssertionText() + "\n"
                + "Total number of failures: " + testRunItem.getFailureCount() + "\n"
                + "Report URL: " + "[" + build.getRvfURL() + "|" + build.getRvfURL() + "]" + "\n");
		List<ValidationReport.RvfValidationResult.TestResult.TestRunItem.FailureDetail> firstNInstances = getFirstNInstances(testRunItem.getFirstNInstances(), 10);
		if (!firstNInstances.isEmpty()) {
			result.append("First ").append(firstNInstances.size()).append(" failures: \n");
			for (ValidationReport.RvfValidationResult.TestResult.TestRunItem.FailureDetail failureDetail: firstNInstances) {
				result.append("* ").append(failureDetail.toStringAndTruncateIfTextTooLong()).append("\n");
			}
		}
		return result.toString();
	}

	private String getPrettyString(String input) {
		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(input);
		return gson.toJson(je);
	}

	private List<ValidationReport.RvfValidationResult.TestResult.TestRunItem.FailureDetail> getFirstNInstances(List<ValidationReport.RvfValidationResult.TestResult.TestRunItem.FailureDetail> instances, int numberOfItem) {
		if (instances == null) {
			return Collections.emptyList();
		}
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

	private String createJiraIssue(String summary, String description) throws BusinessServiceException {
		try {
			JSONObject issue = jiraCloudClient.createIssue(project, summary, description, issueType, reporter);
			String issueKey = issue.getString("key");
			logger.info("New JIRA ticket with key {} has been created", issueKey);
			return issueKey;
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to create Jira ticket. Error: " + extractJiraException(e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
		}
	}

	private void updateJiraIssue(Product product, String releaseDate, String issueKey) throws BusinessServiceException {
		ReleaseCenter releaseCenter = product.getReleaseCenter();
		try {
			JSONObject issueFields = new JSONObject();
			issueFields.put(productReleaseDate, releaseDate);

			JSONObject reportingEntityField = new JSONObject();
			reportingEntityField.put(VALUE, reportingEntityDefaultValue);
			issueFields.put(reportingEntity, reportingEntityField);

			JSONObject reportingStageField = new JSONObject();
			reportingStageField.put(VALUE, reportingStageDefaultValue);
			issueFields.put(reportingStage, Collections.singletonList(reportingStageField));

			if (StringUtils.hasLength(assignee)) {
				JSONObject assigneeField = new JSONObject();
				assigneeField.put(ACCOUNT_ID, assignee);
				issueFields.put(Field.ASSIGNEE, assigneeField);
			}
			String snomedCtProductValue = StringUtils.hasLength(product.getOverriddenSnomedCtProduct()) ? product.getOverriddenSnomedCtProduct() : releaseCenter.getSnomedCtProduct();
			if (StringUtils.hasLength(snomedCtProductValue)) {
				JSONObject snomedCtProductField = new JSONObject();
				snomedCtProductField.put(VALUE, snomedCtProductValue.trim());
				issueFields.put(snomedCtProduct, snomedCtProductField);
			}
			jiraCloudClient.updateIssue(issueKey, issueFields);
		} catch (IOException e) {
			throw new BusinessServiceException("Jira ticket has been created successfully but failed to update. Error: " + extractJiraException(e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
		}
	}

	private String extractJiraException(String message) {
		if (StringUtils.hasLength(message)) {
			String patternStr = "\"%s\":\"Option id 'null' is not valid\"";
			String invalidCustomField = "";
			for (String field : jiraCustomFields.keySet()) {
				Pattern pattern = Pattern.compile(String.format(patternStr, field));
				Matcher matcher = pattern.matcher(message);
				boolean matchFound = matcher.find();
				if(matchFound) {
					invalidCustomField = field;
					break;
				}
			}
			if (StringUtils.hasLength(invalidCustomField)) {
				return String.format("The JIRA custom field '%s' is empty or invalid. Please contact Admin for support.", jiraCustomFields.get(invalidCustomField));
			}
		}
		return message;
	}

	private ValidationReport getRVFReport(String url) throws IOException {
		RestTemplate rvfRestTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.add("Cookie", SecurityUtil.getAuthenticationToken());
		HttpEntity<String> entity = new HttpEntity<>(headers);
		ResponseEntity<String> response = rvfRestTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		String validationReportString = response.getBody();
		if (StringUtils.hasLength(validationReportString)) {
			validationReportString = validationReportString.replace("\"TestResult\"", "\"testResult\"");
		}

		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().failOnUnknownProperties(false).build();
		return objectMapper.readValue(validationReportString, ValidationReport.class);
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
						private static final int FULL_COMPONENT_MAX_LENGTH = 1000;
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

						public String toStringAndTruncateIfTextTooLong() {
							return "{\n\t" +
									"\"conceptId\": " + (conceptId != null ? '\"' + conceptId + '\"' : null) + ",\n\t" +
									"\"conceptFsn\": " + (conceptFsn != null ? '\"' + conceptFsn + '\"' : null) + ",\n\t" +
									"\"detail\": " + (detail != null ? '\"' + detail + '\"' : null) + ",\n\t" +
									"\"componentId\": " + (componentId != null ? '\"' + componentId + '\"' : null) + ",\n\t" +
									"\"fullComponent\": " + (fullComponent != null ? '\"' + (fullComponent.length() <= FULL_COMPONENT_MAX_LENGTH ? fullComponent : fullComponent.substring(0, FULL_COMPONENT_MAX_LENGTH)) + "..." + '\"' : null) + "\n" +
									"}";
						}
					}
				}
			}
		}
	}
}
