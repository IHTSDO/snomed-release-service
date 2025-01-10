package org.ihtsdo.buildcloud.core.service.build.compare.type;

import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.service.PublishService;
import org.ihtsdo.buildcloud.core.service.build.compare.BuildComparisonManager;
import org.ihtsdo.buildcloud.core.service.build.compare.ComponentComparison;
import org.ihtsdo.buildcloud.core.service.build.compare.DefaultComponentComparisonReport;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class BuildStatusComparison extends ComponentComparison {

    @Override
    public String getTestName() {
        return BuildComparisonManager.TestType.BUILD_STATUS_TEST.getLabel();
    }

    @Override
    public String getTestNameShortname() {
        return BuildComparisonManager.TestType.BUILD_STATUS_TEST.name();
    }

    @Override
    public int getTestOrder() {
        return BuildComparisonManager.TestType.BUILD_STATUS_TEST.getTestOrder();
    }

    @Override
    public void findDiff(Build leftBuild, Build rightBuild) throws IOException {
        DefaultComponentComparisonReport dto = new DefaultComponentComparisonReport();
        dto.setName("Status");
        dto.setExpected(leftBuild.getStatus());
        dto.setActual(rightBuild.getStatus());
        if (leftBuild.getStatus().equals(rightBuild.getStatus())) {
            pass(dto);
        } else {
            fail(dto);
        }
    }

    @Override
    public ComponentComparison newInstance(BuildDAO buildDAO, PublishService publishService, String releaseValidationFrameworkUrl) {
        return new BuildStatusComparison();
    }
}
