package org.ihtsdo.buildcloud.dao.s3;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3ClientFactory {

	private S3Client onlineImplementation;
	private S3Client offlineImplementation;

	private static final Logger LOGGER = LoggerFactory.getLogger(S3ClientFactory.class);

	public S3Client getClient(boolean offlineMode) {
		if (offlineMode) {
			LOGGER.info("Using OFFLINE S3 store");
			return offlineImplementation;
		} else {
			LOGGER.info("Using online S3 store");
			return onlineImplementation;
		}
	}

	public void setOnlineImplementation(S3Client onlineImplementation) {
		this.onlineImplementation = onlineImplementation;
	}

	public void setOfflineImplementation(S3Client offlineImplementation) {
		this.offlineImplementation = offlineImplementation;
	}

}
