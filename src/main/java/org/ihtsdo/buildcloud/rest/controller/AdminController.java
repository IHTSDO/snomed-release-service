package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.ihtsdo.buildcloud.core.service.ProductService;
import org.ihtsdo.buildcloud.core.service.ReleaseService;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdmin;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManager;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.text.ParseException;

@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
@RestController
@RequestMapping("/centers/{releaseCenterKey}/products")
@Tag(name = "Admin", description = "-")
public class AdminController {

	@Autowired
	private ProductService productService;

	@Autowired
	private ReleaseService releaseService;

	@PostMapping(value = "/{productKey}/new-authoring-cycle")
	@IsAuthenticatedAsAdmin
	@Operation(summary = "Start new authoring cycle",
			description = "This API is for Daily Build only")
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

	@PostMapping(value = "/{productKey}/upgrade-dependant-version")
	@IsAuthenticatedAsAdminOrReleaseManager
	@Operation(summary = "Upgrade dependant version for daily build product",
			description = "This API is for Daily Build only")
	public ResponseEntity<Void> upgradeDependantVersion(@PathVariable String releaseCenterKey, @PathVariable String productKey) throws BusinessServiceException {
		productService.upgradeDependantVersion(releaseCenterKey.trim(), productKey.trim());
		return ResponseEntity.status(HttpStatus.OK).build();
	}
}
