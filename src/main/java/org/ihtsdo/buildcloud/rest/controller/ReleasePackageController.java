package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.snomed.module.storage.ModuleMetadata;
import org.snomed.module.storage.ModuleStorageCoordinator;
import org.snomed.module.storage.ModuleStorageCoordinatorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Release Package")
@RestController
@RequestMapping(produces={MediaType.APPLICATION_JSON_VALUE})
public class ReleasePackageController {

    @Autowired
    private ModuleStorageCoordinator moduleStorageCoordinator;

    @GetMapping(value = "/releases")
    @Operation(summary = "Returns the list all release packages")
    @ResponseBody
    public Map<String, List<ModuleMetadata>> getAllReleases(
            @RequestParam(value = "page", required = false) String page,
            @RequestParam(value = "size", required = false) String size
    ) throws ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.InvalidArgumentsException {
        return moduleStorageCoordinator.getAllReleases(asIntegerOrFallback(page, 1), asIntegerOrFallback(size, 6));
    }

    private Integer asIntegerOrFallback(String integer, Integer fallback) {
        if (integer == null || integer.isEmpty()) {
            return fallback;
        }

        try {
            return Integer.parseInt(integer);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
