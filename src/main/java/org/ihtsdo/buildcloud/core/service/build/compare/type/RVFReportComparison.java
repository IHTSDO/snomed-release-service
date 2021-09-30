package org.ihtsdo.buildcloud.core.service.build.compare.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.service.build.compare.BuildComparisonManager;
import org.ihtsdo.buildcloud.core.service.build.compare.ComponentComparison;
import org.ihtsdo.buildcloud.core.service.build.compare.DefaultComponentComparisonReport;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class RVFReportComparison extends ComponentComparison {

    public static final String REPORTS = "reports";

    public enum RVFTestName {
        REPORT_URL("RVF report URL"), STATUS("RVF status"), TOTAL_FAILURES("RVF total failures"), TOTAL_WARNINGS("RVF total warnings");

        private final String label;

        RVFTestName(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    @Override
    public String getTestName() {
        return BuildComparisonManager.TestType.RVF_REPORT_TEST.getLabel();
    }

    @Override
    public String getTestNameShortname() {
        return BuildComparisonManager.TestType.RVF_REPORT_TEST.name();
    }

    @Override
    public int getTestOrder() {
        return BuildComparisonManager.TestType.RVF_REPORT_TEST.getTestOrder();
    }

    @Override
    public void findDiff(Build leftBuild, Build rightBuild) throws IOException {
        RestTemplate rvfRestTemplate = new RestTemplate();
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().failOnUnknownProperties(false).build();

        List<DefaultComponentComparisonReport> reports = new ArrayList<>();
        if (!StringUtils.isEmpty(leftBuild.getRvfURL()) && !StringUtils.isEmpty(rightBuild.getRvfURL())) {
            String leftValidationReportString = rvfRestTemplate.getForObject(leftBuild.getRvfURL(), String.class);
            if (!StringUtils.isEmpty(leftValidationReportString)) {
                leftValidationReportString = leftValidationReportString.replace("\"TestResult\"", "\"testResult\"");
            }
            final ValidationReport leftValidationReport = objectMapper.readValue(leftValidationReportString, ValidationReport.class);

            String rightValidationReportString = rvfRestTemplate.getForObject(rightBuild.getRvfURL(), String.class);
            if (!StringUtils.isEmpty(rightValidationReportString)) {
                rightValidationReportString = rightValidationReportString.replace("\"TestResult\"", "\"testResult\"");
            }
            final ValidationReport rightValidationReport = objectMapper.readValue(rightValidationReportString, ValidationReport.class);
            if (leftValidationReport.getStatus() != null && rightValidationReport.getStatus() != null && !leftValidationReport.getStatus().equals(rightValidationReport.getStatus())) {
                DefaultComponentComparisonReport dto = new DefaultComponentComparisonReport();
                dto.setName(RVFTestName.STATUS.getLabel());
                dto.setStatus(BuildComparisonManager.ComparisonState.FAILED.name());
                dto.setExpected(leftValidationReport.getStatus());
                dto.setActual(rightValidationReport.getStatus());
                reports.add(dto);
            }
            if (totalFailurePresent(leftValidationReport) && totalFailurePresent(rightValidationReport) && getTotalFailures(leftValidationReport) != getTotalFailures(rightValidationReport)) {
                DefaultComponentComparisonReport dto = new DefaultComponentComparisonReport();
                dto.setName(RVFTestName.TOTAL_FAILURES.getLabel());
                dto.setStatus(BuildComparisonManager.ComparisonState.FAILED.name());
                dto.setExpected(getTotalFailures(leftValidationReport));
                dto.setActual(getTotalFailures(rightValidationReport));
                reports.add(dto);
            }
            if (totalWarningPresent(leftValidationReport) && totalWarningPresent(rightValidationReport) && getTotalWarnings(leftValidationReport) != getTotalWarnings(rightValidationReport)) {
                DefaultComponentComparisonReport dto = new DefaultComponentComparisonReport();
                dto.setName(RVFTestName.TOTAL_WARNINGS.getLabel());
                dto.setStatus(BuildComparisonManager.ComparisonState.FAILED.name());
                dto.setExpected(getTotalWarnings(leftValidationReport));
                dto.setActual(getTotalWarnings(rightValidationReport));
                reports.add(dto);
            }
            if (reports.size() > 0) {
                fail(reports);
            } else {
                pass();
            }
        } else {
            if ((StringUtils.isEmpty(leftBuild.getRvfURL()) && !StringUtils.isEmpty(rightBuild.getRvfURL()))
                || (!StringUtils.isEmpty(leftBuild.getRvfURL()) && StringUtils.isEmpty(rightBuild.getRvfURL()))){
                DefaultComponentComparisonReport dto = new DefaultComponentComparisonReport();
                dto.setName(RVFTestName.REPORT_URL.getLabel());
                dto.setStatus(BuildComparisonManager.ComparisonState.NOT_FOUND.name());
                dto.setExpected(!StringUtils.isEmpty(leftBuild.getRvfURL()) ? leftBuild.getRvfURL() : null);
                dto.setActual(!StringUtils.isEmpty(rightBuild.getRvfURL()) ? rightBuild.getRvfURL() : null);
                reports.add(dto);
                fail(reports);
            }
        }
    }
    private boolean testResultFound(ValidationReport report) {
        return report != null && report.getRvfValidationResult() != null && report.getRvfValidationResult().getTestResult() != null;
    }

    private boolean totalFailurePresent(ValidationReport report) {
        return testResultFound(report) && report.getRvfValidationResult().getTestResult().getTotalFailures() != null;
    }

    private boolean totalWarningPresent(ValidationReport report) {
        return testResultFound(report) && report.getRvfValidationResult().getTestResult().getTotalWarnings() != null;
    }

    private int getTotalWarnings(ValidationReport report) {
        return report.getRvfValidationResult().getTestResult().getTotalWarnings();
    }

    private int getTotalFailures(ValidationReport report) {
        return report.getRvfValidationResult().getTestResult().getTotalFailures();
    }

    private static final class ValidationReport {

        public static final String COMPLETE = "COMPLETE";

        private String status;
        private RvfValidationResult rvfValidationResult;

        public boolean isComplete() {
            return COMPLETE.equals(status);
        }

        public Long getContentHeadTimestamp() {
            return rvfValidationResult.getContentHeadTimestamp();
        }

        public boolean hasNoErrorsOrWarnings() {
            return rvfValidationResult.hasNoErrorsOrWarnings();
        }

        public String getStatus() {
            return status;
        }

        public RvfValidationResult getRvfValidationResult() {
            return rvfValidationResult;
        }

        private static final class RvfValidationResult {

            private ValidationConfig validationConfig;
            private TestResult testResult;

            public Long getContentHeadTimestamp() {
                return validationConfig.getContentHeadTimestamp();
            }

            public boolean hasNoErrorsOrWarnings() {
                return getTestResult().getTotalFailures() == 0 && getTestResult().getTotalWarnings() == 0;
            }

            public ValidationConfig getValidationConfig() {
                return validationConfig;
            }

            public TestResult getTestResult() {
                return testResult;
            }

            private static final class ValidationConfig {

                private String contentHeadTimestamp;

                public Long getContentHeadTimestamp() {
                    return contentHeadTimestamp != null ? Long.parseLong(contentHeadTimestamp) : null;
                }
            }

            private static final class TestResult {

                private Integer totalFailures;
                private Integer totalWarnings;

                public Integer getTotalFailures() {
                    return totalFailures;
                }

                public Integer getTotalWarnings() {
                    return totalWarnings;
                }
            }
        }
    }
}
