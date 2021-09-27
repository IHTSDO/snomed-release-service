package org.ihtsdo.buildcloud.rest.controller;

import com.github.difflib.text.DiffRow;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.service.AutomatedTestService;
import org.ihtsdo.buildcloud.core.service.BuildService;
import org.ihtsdo.buildcloud.core.service.build.compare.BuildComparisonReport;
import org.ihtsdo.buildcloud.rest.controller.helper.ControllerHelper;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
@Controller
@Api(value = "Automated Test")
public class AutomatedTestController {

    @Autowired
    private BuildService buildService;

    @Autowired
    private AutomatedTestService automatedTestService;

    @GetMapping(value = "/test-reports")
    @IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
    @ResponseBody
    @ApiOperation(value = "Get all test report")
    public List<BuildComparisonReport> getTestReports() {
        return automatedTestService.getAllTestReports();
    }

    @GetMapping(value = "/centers/{releaseCenterKey}/products/{productKey}/builds/compare/{compareId}")
    @IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
    @ResponseBody
    @ApiOperation(value = "Get test report for specific Id")
    public BuildComparisonReport getTestReport(@PathVariable final String releaseCenterKey,
                                               @PathVariable final String productKey,
                                               @PathVariable final String compareId,
                                               final HttpServletRequest request) {
        return automatedTestService.getTestReport(releaseCenterKey, productKey, compareId);
    }

    @PostMapping(value = "/centers/{releaseCenterKey}/products/{productKey}/builds/compare")
    @IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
    @ResponseBody
    @ApiOperation(value = "Compare 2 builds", notes = "Compare 2 builds and put the report to product")
    public ResponseEntity<Void> compareBuilds(
            @PathVariable final String releaseCenterKey,
            @PathVariable final String productKey,
            @RequestParam final String leftBuildId,
            @RequestParam final String rightBuildId,
            final HttpServletRequest request) {
        // Verify if the builds exist
        Build leftBuild  = buildService.find(releaseCenterKey, productKey, leftBuildId, false, false, false , null);
        Build rightBuild  = buildService.find(releaseCenterKey, productKey, rightBuildId, false, false, false , null);

        String compareId = UUID.randomUUID().toString();
        automatedTestService.compareBuilds(compareId, leftBuild, rightBuild);

        return ControllerHelper.getCreatedResponse(compareId);
    }

    @PostMapping(value = "/centers/{releaseCenterKey}/products/{productKey}/files/find-diff")
    @IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
    @ResponseBody
    @ApiOperation(value = "Compare file from 2 builds", notes = "Compare 2 builds and put the report to product")
    public List<DiffRow> compareFiles(
            @PathVariable final String releaseCenterKey,
            @PathVariable final String productKey,
            @RequestParam final String leftBuildId,
            @RequestParam final String rightBuildId,
            @RequestParam final String fileName,
            final HttpServletRequest request) throws BusinessServiceException {
        // Verify if the builds exist
        Build leftBuild  = buildService.find(releaseCenterKey, productKey, leftBuildId, false, false, false , null);
        Build rightBuild  = buildService.find(releaseCenterKey, productKey, rightBuildId, false, false, false , null);

        return automatedTestService.findDiff(leftBuild, rightBuild, fileName);
    }
}
