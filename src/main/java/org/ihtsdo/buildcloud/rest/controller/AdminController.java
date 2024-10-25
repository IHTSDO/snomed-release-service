package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.core.service.BuildService;
import org.ihtsdo.buildcloud.core.service.ProductService;
import org.ihtsdo.buildcloud.core.service.PublishService;
import org.ihtsdo.buildcloud.core.service.ReleaseService;
import org.ihtsdo.buildcloud.rest.pojo.PostReleaseRequest;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdmin;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManager;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
@Controller
@RequestMapping("/centers/{releaseCenterKey}")
@Tag(name = "Admin", description = "-")
public class AdminController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ReleaseService releaseService;

    @Autowired
    private PublishService publishService;

    @Autowired
    private BuildService buildService;

    @PostMapping(value = "/products/{productKey}/new-authoring-cycle")
    @IsAuthenticatedAsAdmin
    @ResponseBody
    @Operation(summary = "Start new authoring cycle. However, This endpoint will be replaced by the new one /post-release",
            description = "This API is for Daily Build only",
            deprecated = true)
    @Deprecated(forRemoval = true)
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
    @ResponseBody
    @Operation(summary = "Upgrade dependant version for daily build product",
            description = "This API is for Daily Build only")
    public ResponseEntity<Void> upgradeDependantVersion(@PathVariable String releaseCenterKey, @PathVariable String productKey) throws BusinessServiceException, IOException, ParseException, JAXBException {
        productService.upgradeDependantVersion(releaseCenterKey.trim(), productKey.trim());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping(value = "/post-release", consumes = MediaType.ALL_VALUE)
    @IsAuthenticatedAsAdminOrReleaseManager
    @ResponseBody
    public ResponseEntity<Object> doPostReleaseTask(@PathVariable String releaseCenterKey,
                                                    @RequestBody final PostReleaseRequest request) throws IOException, DecoderException, BusinessServiceException {

        Build build = buildService.find(releaseCenterKey, request.releasedSourceProductKey(), request.releasedSourceBuildKey(), null, null, null, null);
        String publishedReleaseFileName = publishService.copyReleaseFileToPublished(build);
        releaseService.startNewAuthoringCycle(releaseCenterKey, request.dailyBuildProductKey(), build, request.nextCycleEffectiveTime(), publishedReleaseFileName, request.newDependencyPackage());
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
