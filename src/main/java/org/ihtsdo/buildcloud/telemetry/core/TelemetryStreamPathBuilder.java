package org.ihtsdo.buildcloud.telemetry.core;

public class TelemetryStreamPathBuilder {

	public static String getS3StreamDestinationPath(String bucketName, String objectKey) {
		return Constants.s3 + Constants.PROTOCOL_SEPARATOR + bucketName + "/" + objectKey;
	}

}
