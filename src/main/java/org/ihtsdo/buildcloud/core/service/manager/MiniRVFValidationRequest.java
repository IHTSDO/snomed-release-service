package org.ihtsdo.buildcloud.core.service.manager;

public final class MiniRVFValidationRequest {

	private final String buildId;
	private final String releaseCenterKey;
	private final String productKey;

	public MiniRVFValidationRequest(final String buildId, final String releaseCenterKey, final String productKey) {
		this.buildId = buildId;
		this.releaseCenterKey = releaseCenterKey;
		this.productKey = productKey;
	}

	public final String getBuildId() {
		return buildId;
	}

	public final String getReleaseCenterKey() {
		return releaseCenterKey;
	}

	public final String getProductKey() {
		return productKey;
	}
}
