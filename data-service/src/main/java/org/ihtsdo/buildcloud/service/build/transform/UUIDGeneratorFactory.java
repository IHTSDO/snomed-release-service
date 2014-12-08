package org.ihtsdo.buildcloud.service.build.transform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UUIDGeneratorFactory {

	private final UUIDGenerator randomGenerator;
	private final UUIDGenerator pseudoGenerator;
	private static final Logger LOGGER = LoggerFactory.getLogger(UUIDGeneratorFactory.class);

	public UUIDGeneratorFactory(final UUIDGenerator randomGenerator, final UUIDGenerator pseudoGenerator) {
		this.randomGenerator = randomGenerator;
		this.pseudoGenerator = pseudoGenerator;
	}

	public UUIDGenerator getInstance(final boolean isOffLine) {
		if (isOffLine) {
		    LOGGER.info("Offline mode and pseudo UUID generator is used instead of random generator.");
			return pseudoGenerator;
		}
		return randomGenerator;
	}

}
