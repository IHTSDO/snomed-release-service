package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.ihtsdo.buildcloud.core.service.ReleaseService;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdmin;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.text.ParseException;

@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
@Controller
@RequestMapping("/centers/{releaseCenterKey}/products")
@Api(value = "Admin")
public class AdminController {

	@Autowired
	private ReleaseService releaseService;

	@PostMapping(value = "/{productKey}/new-authoring-cycle")
	@IsAuthenticatedAsAdmin
	@ResponseBody
	@ApiOperation(value = "Start new authoring cycle", notes = "This API is for Daily Build only")
	public ResponseEntity startNewAuthoringCycle(@PathVariable String releaseCenterKey, @PathVariable String productKey,
												 @ApiParam(value = "New effective time. Required format: yyyy-MM-dd", required = true) @RequestParam String effectiveTime,
												 @ApiParam(value = "The product that contains the latest published build. This param requires a product key", required = true) @RequestParam String productSource,
												 @ApiParam(value = "New dependency package if needed.") @RequestParam(required = false) String dependencyPackage) throws BusinessServiceException, IOException, ParseException, JAXBException {
		try {
			DateFormatUtils.ISO_DATE_FORMAT.parse(effectiveTime);
		} catch (final ParseException e) {
			throw new BadRequestException("Invalid effectiveTime format. Expecting format " + DateFormatUtils.ISO_DATE_FORMAT.getPattern() + ".", e);
		}
		releaseService.startNewAuthoringCycle(releaseCenterKey, productKey, effectiveTime, productSource, dependencyPackage);
		return new ResponseEntity(HttpStatus.OK);
	}
}
