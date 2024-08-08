package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ihtsdo.buildcloud.core.service.ModuleStorageCoordinatorCache;
import org.snomed.module.storage.ModuleMetadata;
import org.snomed.module.storage.ModuleStorageCoordinatorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Tag(name = "Release Package")
@RestController
@RequestMapping(produces={MediaType.APPLICATION_JSON_VALUE})
public class ReleasePackageController {

    @Autowired
    private ModuleStorageCoordinatorCache moduleStorageCoordinatorCache;

    @GetMapping(value = "/releases")
    @Operation(summary = "Returns the list all release packages")
    @ResponseBody
    public Map<String, List<ModuleMetadata>> getAllReleases(
            @RequestParam(value = "page", required = false) String page,
            @RequestParam(value = "size", required = false) String size
    ) throws ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.InvalidArgumentsException {
        return this.getAllReleases(asIntegerOrFallback(page, 1), asIntegerOrFallback(size, 6));
    }

    private Map<String, List<ModuleMetadata>> getAllReleases(int page, int size) throws ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.InvalidArgumentsException {
        Map<String, List<ModuleMetadata>> releases = moduleStorageCoordinatorCache.getAllReleases();
        boolean paging = page >= 1 && size >= 1;
        if (paging) {

            for (Map.Entry<String, List<ModuleMetadata>> stringListEntry : releases.entrySet()) {
                List<ModuleMetadata> moduleMetadataList = stringListEntry.getValue();
                if (moduleMetadataList != null && !moduleMetadataList.isEmpty()) {
                    stringListEntry.setValue(this.subList(moduleMetadataList, page, size));
                }
            }
        }

        return releases;
    }

    private List<ModuleMetadata> subList(List<ModuleMetadata> list, int page, int size) {
        if (size > 0 && page > 0) {
            int fromIndex = (page - 1) * size;
            if (list != null && !list.isEmpty() && list.size() > fromIndex) {
                int toIndex = fromIndex == 0 ? size : fromIndex * size;
                return list.subList(fromIndex, Math.min(toIndex, list.size()));
            } else {
                return Collections.emptyList();
            }
        } else {
            return list;
        }
    }

    private Integer asIntegerOrFallback(String integer, Integer fallback) {
        if (integer == null || integer.trim().isEmpty()) {
            return fallback;
        }

        try {
            return Integer.parseInt(integer);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
