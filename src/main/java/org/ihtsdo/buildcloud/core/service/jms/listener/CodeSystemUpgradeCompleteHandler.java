package org.ihtsdo.buildcloud.core.service.jms.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.ihtsdo.buildcloud.core.entity.ExtensionConfig;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.core.service.ProductService;
import org.ihtsdo.buildcloud.core.service.ReleaseCenterService;
import org.ihtsdo.buildcloud.core.service.helper.FilterOption;
import org.ihtsdo.buildcloud.rest.controller.helper.PageRequestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CodeSystemUpgradeCompleteHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ReleaseCenterService releaseCenterService;

    private final ProductService productService;

    @Autowired
    public CodeSystemUpgradeCompleteHandler(ReleaseCenterService releaseCenterService, ProductService productService) {
        this.releaseCenterService = releaseCenterService;
        this.productService = productService;
    }

    @JmsListener(destination = "${snowstorm.jms.queue.prefix}.upgrade.complete", containerFactory = "topicJmsListenerContainerFactory")
    void messageConsumer(TextMessage textMessage) throws JMSException, JsonProcessingException {
        logger.info("receiveReleasePublishEvent {}", textMessage);
        ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final Map<String, String> message = objectMapper.readValue(textMessage.getText(), Map.class);

        final String codeSystemShortName = message.get("codeSystemShortName");
        final String dependencyPackage = message.get("dependencyPackage");

        List<ReleaseCenter> centers = releaseCenterService.findAll();
        ReleaseCenter releaseCenter = centers.stream().filter(center -> center.getCodeSystem() != null && center.getCodeSystem().equals(codeSystemShortName)).findFirst().orElse(null);
        if (releaseCenter == null) {
            logger.error("No release center is associated with the code system {}", codeSystemShortName);
            return;
        }
        try {
            Set<FilterOption> filterOptions = EnumSet.noneOf(FilterOption.class);
            PageRequest pageRequest = PageRequestHelper.createPageRequest(0, 100, null, null);
            Page<Product> page = productService.findAll(releaseCenter.getBusinessKey(), filterOptions, pageRequest, false);
            if (page.getTotalElements() == 0) return;

            for (Product product : page.getContent()) {
                if (product.getBuildConfiguration().isDailyBuild()) {
                    ExtensionConfig extensionConfig = product.getBuildConfiguration().getExtensionConfig();
                    if (extensionConfig != null) {
                        extensionConfig.setDependencyRelease(dependencyPackage);
                        productService.update(product);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to update the daily build product config after upgrading the  code system {}", codeSystemShortName, e);
        }
    }
}
