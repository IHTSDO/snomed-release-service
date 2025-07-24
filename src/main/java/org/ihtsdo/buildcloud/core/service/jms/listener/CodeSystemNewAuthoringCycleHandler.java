package org.ihtsdo.buildcloud.core.service.jms.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.ihtsdo.buildcloud.core.entity.*;
import org.ihtsdo.buildcloud.core.service.*;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.helper.FilterOption;
import org.ihtsdo.buildcloud.rest.controller.helper.PageRequestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.module.storage.ModuleMetadata;
import org.snomed.module.storage.ModuleStorageCoordinatorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.util.*;

@Component
public class CodeSystemNewAuthoringCycleHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ProductService productService;

    private final BuildService buildService;

    private final ModuleStorageCoordinatorCache moduleStorageCoordinatorCache;

    private final ReleaseService releaseService;

    private final ExternalMaintainedRefsetsService externalMaintainedRefsetsService;

    @Autowired
    public CodeSystemNewAuthoringCycleHandler(ProductService productService, BuildService buildService, ModuleStorageCoordinatorCache moduleStorageCoordinatorCache, ReleaseService releaseService, ExternalMaintainedRefsetsService externalMaintainedRefsetsService) {
        this.productService = productService;
        this.buildService = buildService;
        this.moduleStorageCoordinatorCache = moduleStorageCoordinatorCache;
        this.releaseService = releaseService;
        this.externalMaintainedRefsetsService = externalMaintainedRefsetsService;
    }

    @JmsListener(destination = "${snowstorm.jms.queue.prefix}.code-system.new-authoring-cycle", containerFactory = "topicJmsListenerContainerFactory")
    void messageConsumer(TextMessage textMessage) throws JMSException, JsonProcessingException, ParseException {
        logger.info("receiveCodeSystemNewAuthoringCycleEvent {}", textMessage);
        ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final Map<String, String> message = objectMapper.readValue(textMessage.getText(), Map.class);

        final String codeSystemShortName = message.get("codeSystemShortName");
        final String codeSystemBranchPath = message.get("codeSystemBranchPath");
        final String previousPackage = message.get("previousPackage");
        final String newEffectiveTime = message.get("newEffectiveTime");

        if (newEffectiveTime == null) {
            logger.error("New effective time must not be null");
            return;
        }

        try {
            Product dailyBuildProduct = productService.getDailyBuildProductForCodeSystem(codeSystemShortName);
            if (dailyBuildProduct == null) {
                logger.warn("No daily build product found");
                return;
            }
            BuildConfiguration buildConfiguration = dailyBuildProduct.getBuildConfiguration();
            buildConfiguration.setPreviousPublishedPackage(previousPackage);

            // Format: yyyy-MM-dd or yyyyMMdd
            buildConfiguration.setEffectiveTime(newEffectiveTime.contains("-") ? DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.parse(newEffectiveTime) : RF2Constants.DATE_FORMAT.parse(newEffectiveTime));
            updateExtensionConfig(dailyBuildProduct, previousPackage);
            productService.update(dailyBuildProduct);

            Build previousPublishedBuild = getPublishedBuild(dailyBuildProduct.getReleaseCenter(), codeSystemBranchPath, previousPackage);
            if (previousPublishedBuild == null) {
                logger.warn("No previous published build found");
                return;
            }
            String releaseCenterKey = dailyBuildProduct.getReleaseCenter().getBusinessKey();
            releaseService.copyManifestFileAndReplaceEffectiveTime(releaseCenterKey, dailyBuildProduct.getBusinessKey(), previousPublishedBuild, newEffectiveTime);
            externalMaintainedRefsetsService.copyExternallyMaintainedFiles(releaseCenterKey, previousPublishedBuild.getConfiguration().getEffectiveTimeSnomedFormat(), newEffectiveTime.replace("-", ""), true);
        } catch (Exception e) {
            logger.error("Failed to update the daily build product config after starting the new authoring cycle for code system {}", codeSystemShortName, e);
        }
    }

    private Build getPublishedBuild(ReleaseCenter releaseCenter, String codeSystemBranchPath, String previousPackage) {
        Set<FilterOption> filterOptions = EnumSet.noneOf(FilterOption.class);
        PageRequest pageRequest = PageRequestHelper.createPageRequest(0, 1000, null, null);
        Page<Product> productPage = productService.findAll(releaseCenter.getBusinessKey(), filterOptions, pageRequest, false);
        String[] split = previousPackage.split("_");
        String previousEffectiveTime = split[split.length - 1].substring(0, 8);
        String versionedBranch = codeSystemBranchPath + "/" + previousEffectiveTime.substring(0, 4) + "-" + previousEffectiveTime.substring(4, 6) + "-" + previousEffectiveTime.substring(6, 8);
        for (Product product : productPage.getContent()) {
            if (!product.getBuildConfiguration().isDailyBuild()) {
                Build publishedBuild = buildService.findAllDesc(releaseCenter.getBusinessKey(), product.getBusinessKey(), true, false, false, null).stream()
                        .filter(build -> build.getTags() != null && build.getTags().contains(Build.Tag.PUBLISHED))
                        .filter(build -> build.getConfiguration().getBranchPath() != null && build.getConfiguration().getBranchPath().equals(versionedBranch))
                        .findFirst()
                        .orElse(null);
                if (publishedBuild != null) return publishedBuild;
            }
        }
        return null;
    }

    private void updateExtensionConfig(Product product, String previousPackage) throws ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.InvalidArgumentsException, ParseException {
        ExtensionConfig extensionConfig = product.getBuildConfiguration().getExtensionConfig();
        if (extensionConfig != null && extensionConfig.isReleaseAsAnEdition()) {
            Map<String, List<ModuleMetadata>> allReleasesMap = moduleStorageCoordinatorCache.getAllReleases();
            List<ModuleMetadata> allModuleMetadata = new ArrayList<>();
            allReleasesMap.values().forEach(allModuleMetadata::addAll);
            ModuleMetadata moduleMetadata = allModuleMetadata.stream().filter(item -> item.getFilename().equals(previousPackage)).findFirst().orElse(null);
            if (moduleMetadata != null && !CollectionUtils.isEmpty(moduleMetadata.getDependencies())) {
                extensionConfig.setPreviousEditionDependencyEffectiveDate(RF2Constants.DATE_FORMAT.parse(moduleMetadata.getDependencies().get(0).getEffectiveTimeString()));
            }
        }
    }
}
