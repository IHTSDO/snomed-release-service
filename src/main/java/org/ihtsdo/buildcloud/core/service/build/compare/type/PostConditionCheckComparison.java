package org.ihtsdo.buildcloud.core.service.build.compare.type;

import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.PostConditionCheckReport;
import org.ihtsdo.buildcloud.core.service.build.compare.BuildComparisonManager;
import org.ihtsdo.buildcloud.core.service.build.compare.ComponentComparison;
import org.ihtsdo.buildcloud.core.service.build.compare.DefaultComponentComparisonReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostConditionCheckComparison extends ComponentComparison {

    @Autowired
    private BuildDAO buildDAO;

    @Override
    public String getTestName() {
        return BuildComparisonManager.TestType.POST_CONDITION_TEST.getLabel();
    }

    @Override
    public String getTestNameShortname() {
        return BuildComparisonManager.TestType.POST_CONDITION_TEST.name();
    }

    @Override
    public int getTestOrder() {
        return BuildComparisonManager.TestType.POST_CONDITION_TEST.getTestOrder();
    }

    @Override
    public void findDiff(Build leftBuild, Build rightBuild) throws IOException {
        List<DefaultComponentComparisonReport> reports = new ArrayList<>();
        List<PostConditionCheckReport> leftReport = buildDAO.getPostConditionCheckReport(leftBuild);
        List<PostConditionCheckReport> rightReport = buildDAO.getPostConditionCheckReport(rightBuild);
        if (!CollectionUtils.isEmpty(leftReport) && !CollectionUtils.isEmpty(rightReport)) {
            List<String> leftCheckNames = leftReport.stream().map(item -> item.getPostConditionCheckName()).collect(Collectors.toList());
            List<String> rightCheckNames = rightReport.stream().map(item -> item.getPostConditionCheckName()).collect(Collectors.toList());
            List<PostConditionCheckReport> deletedItems = leftReport.stream().filter(item -> !rightCheckNames.contains(item.getPostConditionCheckName())).collect(Collectors.toList());
            deletedItems.forEach(item -> {
                DefaultComponentComparisonReport dto = new DefaultComponentComparisonReport();
                dto.setName(item.getPostConditionCheckName());
                dto.setStatus(BuildComparisonManager.ComparisonState.DELETED.name());
                dto.setExpected(item);
                dto.setExpected(null);
                reports.add(dto);
            });

            List<PostConditionCheckReport> newItems = rightReport.stream().filter(item -> !leftCheckNames.contains(item.getPostConditionCheckName())).collect(Collectors.toList());
            newItems.forEach(item -> {
                DefaultComponentComparisonReport dto = new DefaultComponentComparisonReport();
                dto.setName(item.getPostConditionCheckName());
                dto.setStatus(BuildComparisonManager.ComparisonState.ADD_NEW.name());
                dto.setExpected(null);
                dto.setExpected(item);
                reports.add(dto);
            });

            for (PostConditionCheckReport leftItem : leftReport) {
                for (PostConditionCheckReport rightItem : rightReport) {
                    if (leftItem.getPostConditionCheckName().equals(rightItem.getPostConditionCheckName())) {
                        if (!leftItem.getResult().equals(rightItem.getResult()) || !leftItem.getMessage().equals(rightItem.getMessage())) {
                            DefaultComponentComparisonReport dto = new DefaultComponentComparisonReport();
                            dto.setName(rightItem.getPostConditionCheckName());
                            dto.setStatus(BuildComparisonManager.ComparisonState.RESULT_MISMATCH.name());
                            dto.setExpected(leftItem.getResult());
                            dto.setExpected(rightItem.getResult());
                            reports.add(dto);
                        }
                    }
                }
            }
        } else {
            if (!CollectionUtils.isEmpty(leftReport) && CollectionUtils.isEmpty(rightReport)
                    || (CollectionUtils.isEmpty(leftReport) && !CollectionUtils.isEmpty(rightReport))) {
                DefaultComponentComparisonReport dto = new DefaultComponentComparisonReport();
                dto.setName("Report File");
                dto.setStatus(BuildComparisonManager.ComparisonState.NOT_FOUND.name());
                dto.setExpected(!CollectionUtils.isEmpty(leftReport) ? leftReport : null);
                dto.setExpected(!CollectionUtils.isEmpty(rightReport) ? rightReport : null);
                reports.add(dto);
            }
        }

        if (reports.size() > 0) {
            fail(reports);
        } else {
            pass();
        }
    }
}
