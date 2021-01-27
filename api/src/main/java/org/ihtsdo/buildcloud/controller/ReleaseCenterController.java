package org.ihtsdo.buildcloud.controller;

import com.mangofactory.swagger.annotations.ApiIgnore;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.security.IsAuthenticatedAsAdmin;
import org.ihtsdo.buildcloud.security.IsAuthenticatedAsAdminOrReleaseManagerOrUser;
import org.ihtsdo.buildcloud.service.PermissionService;
import org.ihtsdo.buildcloud.service.PublishService;
import org.ihtsdo.buildcloud.service.ReleaseCenterService;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.ihtsdo.buildcloud.service.PermissionService.GLOBAL_SUFFIX;
import static org.ihtsdo.buildcloud.service.PermissionService.Role.*;

@Controller
@RequestMapping("/centers")
@Api(value = "Release Center", position = 3)
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

    @RequestMapping(method = RequestMethod.GET)
    @IsAuthenticatedAsAdminOrReleaseManagerOrUser
    @ApiOperation(value = "Returns a list all release center for a logged in user",
            notes = "Returns a list of all release centers visible to the currently logged in user.")
    @ResponseBody
    public List<Map<String, Object>> getReleaseCenters(HttpServletRequest request) {
        List<ReleaseCenter> centers = releaseCenterService.findAll();
        Map rolesMap = permissionService.getRolesForLoggedInUser(SecurityContextHolder.getContext().getAuthentication());
        centers = centers.stream().filter(center ->
                Boolean.getBoolean(rolesMap.get(ADMIN + GLOBAL_SUFFIX).toString())
                        || (Boolean.getBoolean(rolesMap.get(RELEASE_MANAGER + GLOBAL_SUFFIX).toString()) && !StringUtils.isEmpty(center.getCodeSystem()))
                        || ((Set<String>) rolesMap.get(ADMIN)).contains(center.getCodeSystem())
                        || ((Set<String>) rolesMap.get(RELEASE_MANAGER)).contains(center.getCodeSystem())
                        || ((Set<String>) rolesMap.get(USER)).contains(center.getCodeSystem())
        ).collect(Collectors.toList());

        return hypermediaGenerator.getEntityCollectionHypermedia(centers, request, RELEASE_CENTER_LINKS);
    }

    @RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission('ADMIN','global')")
    @ApiOperation(value = "Creates a new Release Center",
            notes = " Creates a new Release Center and returns the newly created release center.")
    public ResponseEntity<Map<String, Object>> createReleaseCenter(@RequestBody(required = false) Map<String, String> json,
                                                                   HttpServletRequest request) throws IOException, EntityAlreadyExistsException {

        String name = json.get("name");
        String shortName = json.get("shortName");
        String codeSystem = json.get("codeSystem");
        ReleaseCenter center = releaseCenterService.create(name, shortName, codeSystem);

        boolean currentResource = false;
        Map<String, Object> entityHypermedia = hypermediaGenerator.getEntityHypermedia(center, currentResource, request, RELEASE_CENTER_LINKS);
        return new ResponseEntity<>(entityHypermedia, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{releaseCenterKey}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @IsAuthenticatedAsAdmin
    @ApiOperation(value = "Updates a release center details",
            notes = "Allows the name, shortName and the visibility of a release center (soft delete) to be changed.   "
                    + "Note that the short name is used in the formation of the ‘business key'")
    @ResponseBody
    public Map<String, Object> updateReleaseCenter(@PathVariable String releaseCenterKey,
                                                   @RequestBody(required = false) Map<String, String> json,
                                                   HttpServletRequest request) throws ResourceNotFoundException {

        ReleaseCenter center = releaseCenterService.find(releaseCenterKey);
        center.setName(json.get("name"));
        center.setShortName(json.get("shortName"));
        center.setCodeSystem(json.get("codeSystem"));
        center.setRemoved("true".equalsIgnoreCase(json.get("removed")));
        releaseCenterService.update(center);
        boolean currentResource = false;
        return hypermediaGenerator.getEntityHypermedia(center, currentResource, request, RELEASE_CENTER_LINKS);
    }

    @RequestMapping(value = "/{releaseCenterKey}", method = RequestMethod.GET)
    @IsAuthenticatedAsAdminOrReleaseManagerOrUser
    @ApiOperation(value = "Returns a single release center",
            notes = "Returns a single release center for a given releaseCenterBusinessKey")
    @ResponseBody
    public Map<String, Object> getReleaseCenter(@PathVariable String releaseCenterKey, HttpServletRequest request) throws ResourceNotFoundException {
        ReleaseCenter center = getReleaseCenterRequired(releaseCenterKey);

        boolean currentResource = true;
        return hypermediaGenerator.getEntityHypermedia(center, currentResource, request, RELEASE_CENTER_LINKS);
    }

    @RequestMapping(value = "/{releaseCenterKey}/published", method = RequestMethod.GET)
    @IsAuthenticatedAsAdmin
    @ApiOperation(value = "Returns a list published releases names",
            notes = "Returns a list published releases names for a given release center")
    @ResponseBody
    public Map<String, Object> getReleaseCenterPublishedPackages(@PathVariable String releaseCenterKey, HttpServletRequest request) throws ResourceNotFoundException {

        ReleaseCenter center = getReleaseCenterRequired(releaseCenterKey);

        List<String> publishedPackages = publishService.getPublishedPackages(center);
        Map<String, Object> representation = new HashMap<>();
        representation.put("publishedPackages", publishedPackages);
        return hypermediaGenerator.getEntityHypermedia(representation, true, request);
    }

    @RequestMapping(value = "/{releaseCenterKey}/published", method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
    @IsAuthenticatedAsAdmin
    @ResponseBody
    @ApiIgnore
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
