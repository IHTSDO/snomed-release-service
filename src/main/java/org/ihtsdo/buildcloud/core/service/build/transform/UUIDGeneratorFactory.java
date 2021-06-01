package org.ihtsdo.buildcloud.core.service.build.transform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UUIDGeneratorFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(UUIDGeneratorFactory.class);

	@Bean
	public UUIDGenerator uuidGenerator(@Autowired final RandomUUIDGenerator randomUUIDGenerator,
			@Autowired final PesudoUUIDGenerator pseudoGenerator, @Value("${srs.build.offlineMode}") final boolean isOffLine) {
		if (isOffLine) {
		    LOGGER.info("Offline mode and pseudo UUID generator is used instead of random generator.");
			return pseudoGenerator;
		}
		return randomUUIDGenerator;
	}
}
