package org.ihtsdo.buildcloud.core.service.build.compare.type;

import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.helper.S3PathHelper;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.PreConditionCheckReport;
import org.ihtsdo.buildcloud.core.service.PublishService;
import org.ihtsdo.buildcloud.core.service.build.compare.BuildComparisonManager;
import org.ihtsdo.buildcloud.core.service.build.compare.ComponentComparison;
import org.ihtsdo.buildcloud.core.service.build.compare.DefaultComponentComparisonReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PreConditionCheckComparison extends ComponentComparison {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreConditionCheckComparison.class);

    private final BuildDAO buildDAO;

    private final PublishService publishService;

    @Autowired
    public PreConditionCheckComparison(BuildDAO buildDAO, PublishService publishService) {
        this.buildDAO = buildDAO;
        this.publishService = publishService;
    }

    @Override
    public String getTestName() {
        return BuildComparisonManager.TestType.PRE_CONDITION_TEST.getLabel();
    }

    @Override
    public String getTestNameShortname() {
        return BuildComparisonManager.TestType.PRE_CONDITION_TEST.name();
    }

    @Override
    public int getTestOrder() {
        return BuildComparisonManager.TestType.PRE_CONDITION_TEST.getTestOrder();
    }

    @Override
    public void findDiff(Build leftBuild, Build rightBuild) {
        List<DefaultComponentComparisonReport> reports = new ArrayList<>();
        List<PreConditionCheckReport> leftReport = null;
        List<PreConditionCheckReport> rightReport = null;
        try {
            leftReport = getPreConditionCheckReport(leftBuild);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        try {
            rightReport = getPreConditionCheckReport(rightBuild);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        if (!CollectionUtils.isEmpty(leftReport) && !CollectionUtils.isEmpty(rightReport)) {
            List<String> leftCheckNames = leftReport.stream().map(PreConditionCheckReport::getPreConditionCheckName).toList();
            List<String> rightCheckNames = rightReport.stream().map(PreConditionCheckReport::getPreConditionCheckName).toList();
            List<PreConditionCheckReport> deletedItems = leftReport.stream().filter(item -> !rightCheckNames.contains(item.getPreConditionCheckName())).toList();
            deletedItems.forEach(item -> {
                DefaultComponentComparisonReport dto = new DefaultComponentComparisonReport();
                dto.setName(item.getPreConditionCheckName());
                dto.setStatus(BuildComparisonManager.ComparisonState.DELETED.name());
                dto.setExpected(item);
                dto.setActual(null);
                reports.add(dto);
            });

            List<PreConditionCheckReport> newItems = rightReport.stream().filter(item -> !leftCheckNames.contains(item.getPreConditionCheckName())).toList();
            newItems.forEach(item -> {
                DefaultComponentComparisonReport dto = new DefaultComponentComparisonReport();
                dto.setName(item.getPreConditionCheckName());
                dto.setStatus(BuildComparisonManager.ComparisonState.ADD_NEW.name());
                dto.setExpected(null);
                dto.setActual(item);
                reports.add(dto);
            });

            for (PreConditionCheckReport leftItem : leftReport) {
                for (PreConditionCheckReport rightItem : rightReport) {
                    if (leftItem.getPreConditionCheckName().equals(rightItem.getPreConditionCheckName())) {
                        if (!leftItem.getResult().equals(rightItem.getResult()) || !leftItem.getMessage().equals(rightItem.getMessage())) {
                            DefaultComponentComparisonReport dto = new DefaultComponentComparisonReport();
                            dto.setName(rightItem.getPreConditionCheckName());
                            dto.setStatus(BuildComparisonManager.ComparisonState.RESULT_MISMATCH.name());
                            dto.setExpected(leftItem.getResult());
                            dto.setActual(rightItem.getResult());
                            reports.add(dto);
                        }
                    }
                }
            }
            if (reports.size() > 0) {
                fail(reports);
            } else {
                pass();
            }
        } else {
            if ((!CollectionUtils.isEmpty(leftReport) && CollectionUtils.isEmpty(rightReport))
                || (CollectionUtils.isEmpty(leftReport) && !CollectionUtils.isEmpty(rightReport))) {
                DefaultComponentComparisonReport dto = new DefaultComponentComparisonReport();
                dto.setName("Report File");
                dto.setStatus(BuildComparisonManager.ComparisonState.NOT_FOUND.name());
                dto.setExpected(!CollectionUtils.isEmpty(leftReport) ? leftReport : null);
                dto.setActual(!CollectionUtils.isEmpty(rightReport) ? rightReport : null);
                reports.add(dto);
                fail(reports);
            }
        }
    }

    @Override
    public ComponentComparison newInstance(BuildDAO buildDAO, PublishService publishService, String releaseValidationFrameworkUrl) {
        return new PreConditionCheckComparison(buildDAO, publishService);
    }

    private List<PreConditionCheckReport> getPreConditionCheckReport(Build build) throws IOException {
        List<PreConditionCheckReport> report = new ArrayList<>();
        Build found = buildDAO.find(build.getReleaseCenterKey(), build.getProductKey(), build.getId(), false, false, false, null);
        if (found != null) {
            // Trying to find the report file from build folder
            report = buildDAO.getPreConditionCheckReport(build);
        } else {
            // Trying to find the report file from published folder
            Map<String, String> publishedBuildPathMap = publishService.getPublishedBuildPathMap(build.getReleaseCenterKey(), build.getProductKey());
            if (publishedBuildPathMap.containsKey(build.getId())) {
                String absoluteBuildPath = publishedBuildPathMap.get(build.getId());
                String sourceBuildPath = absoluteBuildPath.substring(absoluteBuildPath.indexOf(S3PathHelper.SEPARATOR) + 1);
                String sourceBucketName = absoluteBuildPath.substring(0, absoluteBuildPath.indexOf(S3PathHelper.SEPARATOR));
                report = buildDAO.getPreConditionCheckReport(sourceBucketName, sourceBuildPath + S3PathHelper.PRE_CONDITION_CHECKS_REPORT);
            }
        }

        return report;
    }
}
