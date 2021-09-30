package org.ihtsdo.buildcloud.core.service.build.compare;

import java.util.List;

public class FileDiffReport {
	public enum Status {
		RUNNING, COMPLETED, FAILED;
	}

	private String fileName;
	private String leftBuildId;
	private String rightBuildId;
	private Status status;
	private List<DiffRow> diffRows;

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getLeftBuildId() {
		return leftBuildId;
	}

	public void setLeftBuildId(String leftBuildId) {
		this.leftBuildId = leftBuildId;
	}

	public String getRightBuildId() {
		return rightBuildId;
	}

	public void setRightBuildId(String rightBuildId) {
		this.rightBuildId = rightBuildId;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public List<DiffRow> getDiffRows() {
		return diffRows;
	}

	public void setDiffRows(List<DiffRow> diffRows) {
		this.diffRows = diffRows;
	}
}
