package org.ihtsdo.buildcloud.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "codesystem.module.storage")
public class CodeSystemModuleStorageConfiguration {
	private Map<String, String> config;

	public Map<String, String> getConfig() {
		return config;
	}

	public void setConfig(Map<String, String> config) {
		this.config = config;
	}

	public String getCodeSystemShortname(String codeSystem) {
		return config.get(codeSystem);
	}
}
