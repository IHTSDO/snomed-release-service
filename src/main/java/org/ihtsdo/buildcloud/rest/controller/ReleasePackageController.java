package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.snomed.module.storage.ModuleMetadata;
import org.snomed.module.storage.ModuleStorageCoordinator;
import org.snomed.module.storage.ModuleStorageCoordinatorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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
    public Map<String, List<ModuleMetadata>> getAllReleases() throws ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.InvalidArgumentsException {
        return moduleStorageCoordinator.getAllReleases();
    }
}
