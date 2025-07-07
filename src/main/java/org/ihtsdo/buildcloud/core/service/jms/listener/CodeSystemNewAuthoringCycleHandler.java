package org.ihtsdo.buildcloud.core.service.jms.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.ihtsdo.buildcloud.core.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.core.entity.ExtensionConfig;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.service.ModuleStorageCoordinatorCache;
import org.ihtsdo.buildcloud.core.service.ProductService;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.module.storage.ModuleMetadata;
import org.snomed.module.storage.ModuleStorageCoordinatorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CodeSystemNewAuthoringCycleHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ProductService productService;

    private final ModuleStorageCoordinatorCache moduleStorageCoordinatorCache;

    @Autowired
    public CodeSystemNewAuthoringCycleHandler(ProductService productService, ModuleStorageCoordinatorCache moduleStorageCoordinatorCache) {
        this.productService = productService;
        this.moduleStorageCoordinatorCache = moduleStorageCoordinatorCache;
    }

    @JmsListener(destination = "${snowstorm.jms.queue.prefix}.code-system.new-authoring-cycle", containerFactory = "topicJmsListenerContainerFactory")
    void messageConsumer(TextMessage textMessage) throws JMSException, JsonProcessingException, ParseException {
        logger.info("receiveCodeSystemNewAuthoringCycleEvent {}", textMessage);
        ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final Map<String, String> message = objectMapper.readValue(textMessage.getText(), Map.class);

        final String codeSystemShortName = message.get("codeSystemShortName");
        final String previousPackage = message.get("previousPackage");
        final String dependencyPackage = message.get("dependencyPackage");
        final String newEffectiveTime = message.get("newEffectiveTime");
        try {
            Product dailyBuildProduct = productService.getDailyBuildProductForCodeSystem(codeSystemShortName);
            if (dailyBuildProduct == null) return;
            BuildConfiguration buildConfiguration = dailyBuildProduct.getBuildConfiguration();
            buildConfiguration.setPreviousPublishedPackage(previousPackage);
            if (newEffectiveTime != null) {
                // Format: yyyy-MM-dd or yyyyMMdd
                buildConfiguration.setEffectiveTime(newEffectiveTime.contains("-") ? DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.parse(newEffectiveTime) : RF2Constants.DATE_FORMAT.parse(newEffectiveTime));
            }
            updateExtensionConfig(dailyBuildProduct, dependencyPackage, previousPackage);
            productService.update(dailyBuildProduct);
        } catch (Exception e) {
            logger.error("Failed to update the daily build product config after starting the new authoring cycle for code system {}", codeSystemShortName, e);
        }
    }

    private void updateExtensionConfig(Product product, String dependencyPackage, String previousPackage) throws ModuleStorageCoordinatorException.OperationFailedException, ModuleStorageCoordinatorException.ResourceNotFoundException, ModuleStorageCoordinatorException.InvalidArgumentsException, ParseException {
        ExtensionConfig extensionConfig = product.getBuildConfiguration().getExtensionConfig();
        if (extensionConfig != null) {
            extensionConfig.setDependencyRelease(dependencyPackage);
            if (extensionConfig.isReleaseAsAnEdition()) {
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
}
