package org.ihtsdo.buildcloud.service.identifier.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdServiceRestClientFactory {

	@Bean
	public IdServiceRestClient idRestClient(@Autowired final IdServiceRestClientImpl onlineImplementation,
			@Autowired final IdServiceRestClientOfflineDemoImpl offlineImplementation,
			@Value("${srs.build.offlineMode}") final boolean offlineMode) {
		return offlineMode ? offlineImplementation : onlineImplementation;
	}
}
