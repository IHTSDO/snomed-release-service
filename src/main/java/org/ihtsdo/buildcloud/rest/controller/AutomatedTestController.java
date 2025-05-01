package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.service.AutomatedTestService;
import org.ihtsdo.buildcloud.core.service.BuildService;
import org.ihtsdo.buildcloud.core.service.build.compare.BuildComparisonReport;
import org.ihtsdo.buildcloud.core.service.build.compare.FileDiffReport;
import org.ihtsdo.buildcloud.rest.controller.helper.ControllerHelper;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
@Controller
@Tag(name = "Automated Test", description = "-")
public class AutomatedTestController {

    @Autowired
    private BuildService buildService;

    @Autowired
    private AutomatedTestService automatedTestService;

    @GetMapping(value = "/regression-test/test-reports")
    @ResponseBody
    @Operation(summary = "Get all test reports")
    public List<BuildComparisonReport> getTestReports() {
        return automatedTestService.getAllTestReports();
    }

    @GetMapping(value = "/centers/{releaseCenterKey}/products/{productKey}/builds/compare/{compareId}")
    @ResponseBody
    @Operation(summary = "Get test report for specific id")
    public BuildComparisonReport getTestReport(@PathVariable final String releaseCenterKey,
                                               @PathVariable final String productKey,
                                               @PathVariable final String compareId,
                                               final HttpServletRequest request) {
        return automatedTestService.getTestReport(releaseCenterKey, productKey, compareId);
    }

    @DeleteMapping(value = "/centers/{releaseCenterKey}/products/{productKey}/builds/compare/{compareId}")
    @ResponseBody
    @Operation(summary = "Delete test report for specific id")
    public void deleteTestReport(@PathVariable final String releaseCenterKey,
                                               @PathVariable final String productKey,
                                               @PathVariable final String compareId) throws BusinessServiceException {
        automatedTestService.deleteTestReport(releaseCenterKey, productKey, compareId);
    }

    @PostMapping(value = "/centers/{releaseCenterKey}/products/{productKey}/builds/compare")
    @IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
    @ResponseBody
    @Operation(summary = "Compare 2 builds",
            description = "Compare 2 builds and put the report to product")
    public ResponseEntity<Void> compareBuilds(
            @PathVariable final String releaseCenterKey,
            @PathVariable final String productKey,
            @RequestParam final String leftBuildId,
            @RequestParam final String rightBuildId,
            @RequestParam final boolean readyToPublish,
            final HttpServletRequest request) {
        String compareId = UUID.randomUUID().toString();
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        automatedTestService.compareBuilds(compareId, releaseCenterKey, productKey, leftBuildId, rightBuildId, readyToPublish, authentication);

        return ControllerHelper.getCreatedResponse(compareId);
    }

    @PostMapping(value = "/centers/{releaseCenterKey}/products/{productKey}/files/find-diff")
    @ResponseBody
    @Operation(summary = "Compare file from 2 builds",
            description = "Compare files from 2 builds and put the report to product")
    public ResponseEntity<Void> compareFiles(
            @PathVariable final String releaseCenterKey,
            @PathVariable final String productKey,
            @RequestParam final String leftBuildId,
            @RequestParam final String rightBuildId,
            @RequestParam final String fileName,
            @RequestParam(required = false) String compareId,
            @RequestParam(required = false, defaultValue = "false") boolean ignoreIdComparison,
            final HttpServletRequest request) throws BusinessServiceException {
        // Verify if the builds exist
        Build leftBuild  = buildService.find(releaseCenterKey, productKey, leftBuildId, false, false, false , null);
        Build rightBuild  = buildService.find(releaseCenterKey, productKey, rightBuildId, false, false, false , null);

        if (!StringUtils.hasLength(compareId)) {
            compareId = UUID.randomUUID().toString();
        }
        automatedTestService.compareFiles(leftBuild, rightBuild, fileName, compareId, ignoreIdComparison);
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("fileName", fileName);
        return ControllerHelper.getCreatedResponse(compareId, queryParams);
    }

    @GetMapping(value = "/centers/{releaseCenterKey}/products/{productKey}/files/find-diff/{compareId}")
    @ResponseBody
    public FileDiffReport getFileComparisonReport(
            @PathVariable final String releaseCenterKey,
            @PathVariable final String productKey,
            @PathVariable final String compareId,
            @RequestParam final String fileName,
            @RequestParam(required = false, defaultValue = "false") boolean ignoreIdComparison,
            final HttpServletRequest request) {
        return automatedTestService.getFileDiffReport(releaseCenterKey, productKey, compareId, fileName, ignoreIdComparison);
    }
}
