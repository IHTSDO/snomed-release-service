package org.ihtsdo.buildcloud.core.service.manager;

public record MiniRVFValidationRequest(String buildId, String releaseCenterKey, String productKey) {
}
