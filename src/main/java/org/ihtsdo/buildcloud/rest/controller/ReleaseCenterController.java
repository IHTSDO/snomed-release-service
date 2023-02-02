package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ihtsdo.buildcloud.rest.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdmin;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManager;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsGlobalAdmin;
import org.ihtsdo.buildcloud.core.service.PermissionService;
import org.ihtsdo.buildcloud.core.service.PublishService;
import org.ihtsdo.buildcloud.core.service.ReleaseCenterService;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.ihtsdo.buildcloud.core.service.PermissionService.GLOBAL_ROLE;
import static org.ihtsdo.buildcloud.core.service.PermissionService.Role.*;

@Controller
@RequestMapping("/centers")
@Tag(name = "Release Center", description = "-")
public class ReleaseCenterController {

    @Autowired
    private ReleaseCenterService releaseCenterService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private PublishService publishService;

    @Autowired
    private HypermediaGenerator hypermediaGenerator;

    private static final String[] RELEASE_CENTER_LINKS = {"products", "published"};

    @GetMapping
    @Operation(summary = "Returns a list all release center for a logged in user",
            description = "Returns a list of all release centers visible to the currently logged in user")
    @ResponseBody
    public List<Map<String, Object>> getReleaseCenters(@RequestParam(required = false) boolean includeRemoved, HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Access is denied");
        }

        List<ReleaseCenter> centers = releaseCenterService.findAll();
        Map rolesMap = permissionService.getRolesForLoggedInUser();
        centers = centers.stream().filter(center -> (includeRemoved || !center.isRemoved()) &&
                ((rolesMap.containsKey(GLOBAL_ROLE) && ((Set) rolesMap.get(GLOBAL_ROLE)).contains(RELEASE_ADMIN.name()))
                        || (StringUtils.hasLength(center.getCodeSystem()) &&
                            ((rolesMap.containsKey(GLOBAL_ROLE) && (((Set) rolesMap.get(GLOBAL_ROLE)).contains(RELEASE_MANAGER.name())
                                                                    || ((Set) rolesMap.get(GLOBAL_ROLE)).contains(RELEASE_LEAD.name())))
                            || (rolesMap.containsKey(center.getCodeSystem()) &&
                                        (((Set) rolesMap.get(center.getCodeSystem())).contains(RELEASE_ADMIN.name()) ||
                                         ((Set) rolesMap.get(center.getCodeSystem())).contains(RELEASE_MANAGER.name()) ||
                                         ((Set) rolesMap.get(center.getCodeSystem())).contains(RELEASE_LEAD.name()) ||
                                         ((Set) rolesMap.get(center.getCodeSystem())).contains(RELEASE_USER.name()) ||
                                         ((Set) rolesMap.get(center.getCodeSystem())).contains(AUTHOR.name()))))))
        ).collect(Collectors.toList());

        return hypermediaGenerator.getEntityCollectionHypermedia(centers, request, RELEASE_CENTER_LINKS);
    }

    @PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE)
    @IsAuthenticatedAsGlobalAdmin
    @Operation(summary = "Creates a new Release Center",
            description = "Creates a new Release Center and returns the newly created release center")
    public ResponseEntity<Map<String, Object>> createReleaseCenter(@RequestBody(required = false) Map<String, String> json,
                                                                   HttpServletRequest request) throws IOException, EntityAlreadyExistsException {

        String name = json.get("name");
        String shortName = json.get("shortName");
        String codeSystem = json.get("codeSystem");
        String snomedCtProduct = json.get("snomedCtProduct");
        ReleaseCenter center = releaseCenterService.create(name, shortName, codeSystem, snomedCtProduct);

        boolean currentResource = false;
        Map<String, Object> entityHypermedia = hypermediaGenerator.getEntityHypermedia(center, currentResource, request, RELEASE_CENTER_LINKS);
        return new ResponseEntity<>(entityHypermedia, HttpStatus.CREATED);
    }

    @PutMapping(value = "/{releaseCenterKey}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @IsAuthenticatedAsAdmin
    @Operation(summary = "Updates a release center details",
            description = "Allows the name, shortName and the visibility of a release center (soft delete) to be changed. " +
                    "Note that the short name is used in the formation of the ‘business key'")
    @ResponseBody
    public Map<String, Object> updateReleaseCenter(@PathVariable String releaseCenterKey,
                                                   @RequestBody(required = false) Map<String, String> json,
                                                   HttpServletRequest request) throws ResourceNotFoundException, BusinessServiceException {

        ReleaseCenter center = releaseCenterService.find(releaseCenterKey);
        String codeSystem = json.get("codeSystem");
        if (codeSystem != center.getCodeSystem()) {
            Map rolesMap = permissionService.getRolesForLoggedInUser();
            if (!((Set) rolesMap.get(GLOBAL_ROLE)).contains(RELEASE_ADMIN.name())) {
                throw new BusinessServiceException("You are not allowed to change Code System. Only Admin Global role has possibility to do this.");
            }
        }
        center.setName(json.get("name"));
        center.setShortName(json.get("shortName"));
        center.setCodeSystem(codeSystem);
        center.setSnomedCtProduct(json.get("snomedCtProduct"));
        center.setRemoved("true".equalsIgnoreCase(json.get("removed")));

        releaseCenterService.update(center);
        return hypermediaGenerator.getEntityHypermedia(center, false, request, RELEASE_CENTER_LINKS);
    }

    @GetMapping(value = "/{releaseCenterKey}")
    @IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
    @Operation(summary = "Returns a single release center",
            description = "Returns a single release center for a given releaseCenterBusinessKey")
    @ResponseBody
    public Map<String, Object> getReleaseCenter(@PathVariable String releaseCenterKey, HttpServletRequest request) throws ResourceNotFoundException {
        ReleaseCenter center = getReleaseCenterRequired(releaseCenterKey);
        return hypermediaGenerator.getEntityHypermedia(center, true, request, RELEASE_CENTER_LINKS);
    }

    @GetMapping(value = "/{releaseCenterKey}/published")
    @IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
    @Operation(summary = "Returns a list published releases names",
            description = "Returns a list published releases names for a given release center")
    @ResponseBody
    public Map<String, Object> getReleaseCenterPublishedPackages(@PathVariable String releaseCenterKey, HttpServletRequest request) throws ResourceNotFoundException {

        ReleaseCenter center = getReleaseCenterRequired(releaseCenterKey);

        List<String> publishedPackages = publishService.getPublishedPackages(center);
        Map<String, Object> representation = new HashMap<>();
        representation.put("publishedPackages", publishedPackages);
        return hypermediaGenerator.getEntityHypermedia(representation, true, request);
    }

    @PostMapping(value = "/{releaseCenterKey}/published", consumes = MediaType.ALL_VALUE)
    @IsAuthenticatedAsAdminOrReleaseManager
    @ResponseBody
    @Operation(hidden = true)
    public ResponseEntity<Object> publishReleaseCenterPackage(@PathVariable String releaseCenterKey,
                                                              @RequestParam(value = "file") final MultipartFile file, @RequestParam(value = "isComponentIdPublishingRequired", defaultValue = "true") boolean publishComponentIds) throws BusinessServiceException, IOException {

        ReleaseCenter center = getReleaseCenterRequired(releaseCenterKey);

        try (InputStream inputStream = file.getInputStream()) {
            publishService.publishAdHocFile(center, inputStream, file.getOriginalFilename(), file.getSize(), publishComponentIds);
        }

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    private ReleaseCenter getReleaseCenterRequired(String releaseCenterBusinessKey) throws ResourceNotFoundException {
        ReleaseCenter center = releaseCenterService.find(releaseCenterBusinessKey);
        if (center == null) {
            throw new ResourceNotFoundException("Unable to find release center: " + releaseCenterBusinessKey);
        }
        return center;
    }

}
