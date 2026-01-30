package org.ihtsdo.buildcloud.core.service.helper;

public class SRSConstants {

	public static final String PRODUCT_NAME_KEY = "productName";
	public static final String PRODUCT_KEY = "productKey";
	public static final String RELEASE_CENTER_KEY = "releaseCenterKey";
	public static final String PRODUCT_BUSINESS_KEY = "productBusinessKey";
	public static final String BUILD_STATUS_KEY = "buildStatus";
	public static final String BUILD_ID_KEY = "buildId";
	public static final String RUN_ID_KEY = "runId";
	public static final String STATE_KEY = "state";
	public static final String STORAGE_LOCATION = "storageLocation";
    public static final String RETRY_COUNT = "retryCount";
	/**
	 * Delivery count of the build-job message (e.g. JMSXDeliveryCount).
	 * This helps distinguish message redelivery/interruption handling from "clean" build retries.
	 */
	public static final String MESSAGE_DELIVERY_COUNT = "messageDeliveryCount";
}
