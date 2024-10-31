package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.ihtsdo.buildcloud.core.service.AdminService;
import org.ihtsdo.buildcloud.core.service.ProductService;
import org.ihtsdo.buildcloud.core.service.ReleaseService;
import org.ihtsdo.buildcloud.rest.controller.helper.ControllerHelper;
import org.ihtsdo.buildcloud.rest.pojo.BrowserUpdateJob;
import org.ihtsdo.buildcloud.rest.pojo.BrowserUpdateRequest;
import org.ihtsdo.buildcloud.rest.pojo.PostReleaseRequest;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdmin;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManager;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.text.ParseException;
import java.util.UUID;

@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
@RestController
@RequestMapping("/centers/{releaseCenterKey}")
@Tag(name = "Admin", description = "-")
public class AdminController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ReleaseService releaseService;

    @Autowired
    private AdminService adminService;

    @IsAuthenticatedAsAdmin
    @Operation(summary = "Start new authoring cycle. However, this endpoint will be replaced by the new one '/trigger-post-release-task'",
            description = "This API is for Daily Build only",
            deprecated = true)
    public ResponseEntity<Void> startNewAuthoringCycle(@PathVariable String releaseCenterKey,
                                                       @PathVariable String productKey,
                                                       @Parameter(description = "New effective time. Required format: yyyy-MM-dd", required = true)
                                                       @RequestParam String effectiveTime,
                                                       @Parameter(description = "The product that contains the latest published build. This param requires a product key", required = true)
                                                       @RequestParam String productSource,
                                                       @Parameter(description = "New dependency package if needed.")
                                                       @RequestParam(required = false) String dependencyPackage)
            throws BusinessServiceException, IOException, ParseException, JAXBException {
        try {
            DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.parse(effectiveTime);
        } catch (final ParseException e) {
            throw new BadRequestException("Invalid effectiveTime format. Expecting format " + DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.getPattern() + ".", e);
        }
        releaseService.startNewAuthoringCycle(releaseCenterKey.trim(), productKey.trim(), effectiveTime, productSource.trim(), dependencyPackage != null ? dependencyPackage.trim() : null);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping(value = "/products/{productKey}/upgrade-dependant-version")
    @IsAuthenticatedAsAdminOrReleaseManager
    @Operation(summary = "Upgrade dependant version for daily build product",
            description = "This API is for Daily Build only")
    public ResponseEntity<Void> upgradeDependantVersion(@PathVariable String releaseCenterKey, @PathVariable String productKey) throws BusinessServiceException {
        productService.upgradeDependantVersion(releaseCenterKey.trim(), productKey.trim());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping(value = "/trigger-post-release-task")
    @IsAuthenticatedAsAdminOrReleaseManager
    @Operation(summary = "Run post-release task which will update the daily build for the new authoring cycle")
    public ResponseEntity<Void> triggerPostReleaseTask(@PathVariable String releaseCenterKey,
                                                   @RequestBody PostReleaseRequest request) throws BusinessServiceException, RestClientException, IOException {
        adminService.runPostReleaseTask(releaseCenterKey, request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping(value = "/trigger-browser-update")
    @IsAuthenticatedAsAdminOrReleaseManager
    @Operation(summary = "Trigger the browser update job",
            description = "The job will be trying to create a new version in the Browser Update box.")
    public ResponseEntity<Void> triggerBrowserUpdate(@PathVariable String releaseCenterKey,
                                                     @RequestBody BrowserUpdateRequest request) {
        String browserUpdateId = UUID.randomUUID().toString();
        adminService.triggerBrowserUpdate(releaseCenterKey, request, SecurityContextHolder.getContext(), browserUpdateId);
        return ControllerHelper.getCreatedResponse(browserUpdateId);
    }

    @GetMapping(value = "/trigger-browser-update/{jobId}")
    @IsAuthenticatedAsAdminOrReleaseManager
    @Operation(summary = "Retrieve a browser update job.",
            description = "Retrieves the latest state of a browser update job. Used to check its status.")
    public ResponseEntity<BrowserUpdateJob> triggerBrowserUpdate(@PathVariable String releaseCenterKey,
                                                                 @PathVariable String jobId) {
        return new ResponseEntity<>(adminService.getBrowserUpdateJob(jobId), HttpStatus.OK);
    }
}
