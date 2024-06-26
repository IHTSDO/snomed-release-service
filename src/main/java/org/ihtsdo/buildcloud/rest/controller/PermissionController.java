package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.ihtsdo.buildcloud.core.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/permissions")
@Tag(name = "Permissions", description = "-")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @GetMapping(value = "/roles")
    @Operation(summary = "Returns a list all roles for a logged in user",
            description = "Returns a list of all roles visible to the currently logged in user")
    @ResponseBody
    public ResponseEntity<Map<String, Set<String>>> getCurrentRoles(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication() ;
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Access is denied");
        }
        return new ResponseEntity<>(permissionService.getRolesForLoggedInUser(), HttpStatus.OK);
    }
}
