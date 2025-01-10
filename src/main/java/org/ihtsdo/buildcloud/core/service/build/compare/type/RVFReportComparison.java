package org.ihtsdo.buildcloud.core.service.build.compare.type;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.service.PublishService;
import org.ihtsdo.buildcloud.core.service.build.compare.BuildComparisonManager;
import org.ihtsdo.buildcloud.core.service.build.compare.ComponentComparison;
import org.ihtsdo.buildcloud.core.service.build.compare.DefaultComponentComparisonReport;
import org.ihtsdo.buildcloud.core.service.build.compare.ValidationComparisonReport;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
public class RVFReportComparison extends ComponentComparison {

    public static final String REPORTS = "reports";


    private String releaseValidationFrameworkUrl;

    public RVFReportComparison(@Value("${rvf.url}") String releaseValidationFrameworkUrl) {
        this.releaseValidationFrameworkUrl = releaseValidationFrameworkUrl;
    }

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
        List<DefaultComponentComparisonReport> reports = new ArrayList<>();
        if (StringUtils.hasLength(leftBuild.getRvfURL()) && StringUtils.hasLength(rightBuild.getRvfURL())) {
            try {
                String validationComparisonReportString = getValidationComparisonReport(leftBuild.getRvfURL(), rightBuild.getRvfURL());
                ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().failOnUnknownProperties(false).build();
                final ValidationComparisonReport highLevelValidationReport = objectMapper.readValue(validationComparisonReportString, ValidationComparisonReport.class);
                if (ValidationComparisonReport.Status.PASS.equals(highLevelValidationReport.getStatus())) {
                    pass(validationComparisonReportString);
                } else {
                    fail(validationComparisonReportString);
                }
            } catch (BusinessServiceException | InterruptedException e) {
                fail("Failed to compare RVF results. Error: " + e.getMessage());
            }
        } else {
            if ((!StringUtils.hasLength(leftBuild.getRvfURL()) && StringUtils.hasLength(rightBuild.getRvfURL()))
                || (StringUtils.hasLength(leftBuild.getRvfURL()) && !StringUtils.hasLength(rightBuild.getRvfURL()))){
                DefaultComponentComparisonReport dto = new DefaultComponentComparisonReport();
                dto.setName(RVFTestName.REPORT_URL.getLabel());
                dto.setStatus(BuildComparisonManager.ComparisonState.NOT_FOUND.name());
                dto.setExpected(StringUtils.hasLength(leftBuild.getRvfURL()) ? leftBuild.getRvfURL() : null);
                dto.setActual(StringUtils.hasLength(rightBuild.getRvfURL()) ? rightBuild.getRvfURL() : null);
                reports.add(dto);
                fail(reports);
            }
        }
    }

    @Override
    public ComponentComparison newInstance(BuildDAO buildDAO, PublishService publishService, String releaseValidationFrameworkUrl) {
        return new RVFReportComparison(releaseValidationFrameworkUrl);
    }

    private String getValidationComparisonReport(String leftUrl, String rightUrl) throws InterruptedException, BusinessServiceException, JsonProcessingException {
        RestTemplate rvfRestTemplate = new RestTemplate();
        URI uri = rvfRestTemplate.postForLocation(releaseValidationFrameworkUrl + "compare?prospectiveReportUrl=" + rightUrl + "&previousReportUrl=" + leftUrl, null);
        final int pollPeriod = 60 * 1000; // 1 minute
        final int maxPollPeriod = 3600000;

        int count = 0;
        while (true) {
            Thread.sleep(pollPeriod);
            count += pollPeriod;

            String validationReportString = rvfRestTemplate.getForObject(uri.toString(), String.class);
            if (StringUtils.hasLength(validationReportString)) {
                ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().failOnUnknownProperties(false).build();
                final ValidationComparisonReport highLevelValidationReport = objectMapper.readValue(validationReportString, ValidationComparisonReport.class);
                if (!ValidationComparisonReport.Status.RUNNING.equals(highLevelValidationReport.getStatus())) {
                    return validationReportString;
                }
            }

            if (count > maxPollPeriod) {
                throw new BusinessServiceException(String.format("RVF reports %s did not complete comparing within the allotted time (%s minutes).", uri.toString(), maxPollPeriod / (60 * 1000)));
            }
        }
    }
}
