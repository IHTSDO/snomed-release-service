package org.ihtsdo.buildcloud.core.service.build.compare;

import org.ihtsdo.buildcloud.core.entity.Build;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class BuildComparisonManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildComparisonManager.class);

    public enum TestType {
        BUILD_STATUS_TEST("Build Status Comparison", 1),
        PRE_CONDITION_TEST("Pre-Condition Comparison", 2),
        POST_CONDITION_TEST("Post Condition Comparison", 3),
        RELEASE_PACKAGE_TEST("Release Package Comparison", 4),
        RVF_REPORT_TEST("RVF Report Comparison", 5);

        private final String label;

        private final int testOrder;

        TestType(String label, int testOrder) {
            this.label = label;
            this.testOrder = testOrder;
        }

        public String getLabel() {
            return label;
        }

        public int getTestOrder() {
            return testOrder;
        }
    }

    public enum ComparisonState {
        DELETED, ADD_NEW, RESULT_MISMATCH, CONTENT_MISMATCH, NOT_FOUND, FAILED;
    }

    private Comparator<ComponentComparison> orderTestComparator = Comparator.comparing(ComponentComparison::getTestOrder);

    @Autowired
    private List<ComponentComparison> componentComparisonChecks;

    public List<HighLevelComparisonReport> runBuildComparisons(final Build leftBuild, final Build rightBuild) throws IOException {
        List<HighLevelComparisonReport> reports = new ArrayList<>();
        componentComparisonChecks.sort(orderTestComparator);
        for (ComponentComparison thisCheck : componentComparisonChecks) {
            thisCheck.findDiff(leftBuild, rightBuild);
            reports.add(thisCheck.getReport());
        }

        return reports;
    }

    public BuildComparisonManager buildComparisonChecks(ComponentComparison... componentComparisonArray) {
        List<ComponentComparison> componentComparisons = new ArrayList<>();
        Collections.addAll(componentComparisons, componentComparisonArray);
        this.componentComparisonChecks = componentComparisons;
        return this;
    }

    public void setBuildComparisons(List<ComponentComparison> componentComparisonChecks) {
        this.componentComparisonChecks = componentComparisonChecks;
    }
}
