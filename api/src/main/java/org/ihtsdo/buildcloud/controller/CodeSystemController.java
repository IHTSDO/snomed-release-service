package org.ihtsdo.buildcloud.controller;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.service.PermissionService;
import org.ihtsdo.buildcloud.service.PermissionServiceCache;
import org.ihtsdo.buildcloud.service.TermServerService;
import org.ihtsdo.otf.rest.exception.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
@Api(value = "Code Systems")
@RequestMapping(value = "/codesystems", produces = "application/json")
public class CodeSystemController {

    @Autowired
    private TermServerService termServerService;

    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(value = "List code systems",
            notes = "List all code systems.\n" +
                    "forBranch is an optional parameter to find the code system which the specified branch is within.")
    @ResponseBody
    public ResponseEntity listCodeSystems(HttpServletRequest request) {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context != null ? context.getAuthentication() : null;
        if (authentication == null) {
            throw new AuthenticationException("User is not logged in.");
        }


        return new ResponseEntity(termServerService.getCodeSystems(), HttpStatus.OK);
    }

}
