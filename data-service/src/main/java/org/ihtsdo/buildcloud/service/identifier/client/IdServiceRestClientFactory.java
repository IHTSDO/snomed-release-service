package org.ihtsdo.buildcloud.service.identifier.client;




public class IdServiceRestClientFactory {

	private final IdServiceRestClient onlineImplementation;
	private final IdServiceRestClient offlineImplementation;

	public IdServiceRestClientFactory(final IdServiceRestClient onlineImplementation, final IdServiceRestClient offlineImplementation) {
		this.onlineImplementation = onlineImplementation;
		this.offlineImplementation = offlineImplementation;
	}

	public IdServiceRestClient getInstance(final boolean offlineMode) {
		if (offlineMode) {
			return offlineImplementation;
		} else {
			return onlineImplementation;
		}
	}
}
