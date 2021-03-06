package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.ihtsdo.buildcloud.rest.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsGlobalAdmin;
import org.ihtsdo.buildcloud.core.service.PermissionService;
import org.ihtsdo.buildcloud.core.service.PermissionServiceCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/permissions")
@Api(value = "Permissions")
public class PermissionController {

    @Autowired
    private HypermediaGenerator hypermediaGenerator;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private PermissionServiceCache permissionServiceCache;

    @GetMapping(value = "/roles")
    @ApiOperation(value = "Returns a list all roles for a logged in user",
            notes = "Returns a list of all roles visible to the currently logged in user.")
    @ResponseBody
    public ResponseEntity getCurrentRoles(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication() ;
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Access is denied");
        }

        return new ResponseEntity(permissionService.getRolesForLoggedInUser(), HttpStatus.OK);
    }

    @PostMapping(value = "/clearCache")
    @IsAuthenticatedAsGlobalAdmin
    public ResponseEntity clearCache(HttpServletRequest request) {
        permissionServiceCache.clearCache();
        return new ResponseEntity(HttpStatus.OK);
    }
}
