package org.ihtsdo.buildcloud.core.service.jms.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.ihtsdo.buildcloud.core.entity.ExtensionConfig;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CodeSystemUpgradeCompleteHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ProductService productService;

    @Autowired
    public CodeSystemUpgradeCompleteHandler(ProductService productService) {
        this.productService = productService;
    }

    @JmsListener(destination = "${snowstorm.jms.queue.prefix}.upgrade.complete", containerFactory = "topicJmsListenerContainerFactory")
    void messageConsumer(TextMessage textMessage) throws JMSException, JsonProcessingException {
        logger.info("receiveCodeSystemUpgradeCompleteEvent {}", textMessage);
        ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final Map<String, String> message = objectMapper.readValue(textMessage.getText(), Map.class);

        final String codeSystemShortName = message.get("codeSystemShortName");
        final String dependencyPackage = message.get("dependencyPackage");
        try {
            Product dailyBuildProduct = productService.getDailyBuildProductForCodeSystem(codeSystemShortName);
            if (dailyBuildProduct == null) return;
            ExtensionConfig extensionConfig = dailyBuildProduct.getBuildConfiguration().getExtensionConfig();
            if (extensionConfig != null) {
                extensionConfig.setDependencyRelease(dependencyPackage);
                productService.update(dailyBuildProduct);
            }
        } catch (
                Exception e) {
            logger.error("Failed to update the daily build product config after upgrading the  code system {}", codeSystemShortName, e);
        }
    }

}
