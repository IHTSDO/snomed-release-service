package org.ihtsdo.buildcloud.service.buildstatuslistener;

import org.ihtsdo.buildcloud.service.worker.BuildStatus;

public class BuildStatusWithProductDetailsRequest {

	private BuildStatus buildStatus;

	private String productBusinessKey;
	private String productName;

	public BuildStatusWithProductDetailsRequest() {
	}

	private BuildStatusWithProductDetailsRequest(final Builder builder) {
		this.buildStatus = builder.buildStatus;
		this.productBusinessKey = builder.productBusinessKey;
		this.productName = builder.productName;
	}

	public final BuildStatus getBuildStatus() {
		return buildStatus;
	}

	public String getProductBusinessKey() {
		return productBusinessKey;
	}

	public String getProductName() {
		return productName;
	}

	public static Builder newBuilder(final String productName) {
		return new Builder(productName);
	}

	public static class Builder {

		private BuildStatus buildStatus;

		private String productBusinessKey;
		private final String productName;

		private Builder(final String productName) {
			this.productName = productName;
		}

		public final Builder withBuildStatus(final BuildStatus buildStatus) {
			this.buildStatus = buildStatus;
			return this;
		}

		public final Builder withProductBusinessKey(final String productBusinessKey) {
			this.productBusinessKey = productBusinessKey;
			return this;
		}

		public final BuildStatusWithProductDetailsRequest build() {
			return new BuildStatusWithProductDetailsRequest(this);
		}
	}
}
