package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.ihtsdo.buildcloud.core.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
@Controller
@RequestMapping("/cache")
@Tag(name = "Cache", description = "-")
public class CacheController {

    @Autowired
    private CacheService cacheService;

    @PostMapping(value = "/clear-all")
    public ResponseEntity<Void> clearCache(HttpServletRequest request) {
        cacheService.clearAllCache();
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
