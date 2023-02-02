package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ihtsdo.buildcloud.core.service.TermServerService;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystem;
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

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

@Controller
@Tag(name = "Code Systems", description = "-")
@RequestMapping(value = "/codesystems", produces = "application/json")
public class CodeSystemController {

    @Autowired
    private TermServerService termServerService;

    @GetMapping
    @Operation(summary = "List code systems",
            description = "List all code systems")
    @ResponseBody
    public ResponseEntity listCodeSystems(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Access is denied");
        }
        List<CodeSystem> codeSystems = termServerService.getCodeSystems();
        codeSystems.stream().forEach(codeSystem -> codeSystem.setUserRoles(Collections.emptySet()));

        return new ResponseEntity(codeSystems, HttpStatus.OK);
    }

}
