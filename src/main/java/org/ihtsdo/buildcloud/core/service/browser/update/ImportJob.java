package org.ihtsdo.buildcloud.core.service.browser.update;

public class ImportJob {

	public enum ImportStatus {
		WAITING_FOR_FILE, RUNNING, COMPLETED, FAILED;
	}

	private ImportStatus status;

	private String errorMessage;

	public void setStatus(ImportStatus status) {
		this.status = status;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public ImportStatus getStatus() {
		return status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
