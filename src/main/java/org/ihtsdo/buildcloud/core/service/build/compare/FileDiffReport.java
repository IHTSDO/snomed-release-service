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
	private List<DiffRow> deleteRows;
	private List<DiffRow> insertRows;
	private List<DiffRow> changeRows;

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

	public List<DiffRow> getDeleteRows() {
		return deleteRows;
	}

	public void setDeleteRows(List<DiffRow> deleteRows) {
		this.deleteRows = deleteRows;
	}

	public List<DiffRow> getInsertRows() {
		return insertRows;
	}

	public void setInsertRows(List<DiffRow> insertRows) {
		this.insertRows = insertRows;
	}

	public List<DiffRow> getChangeRows() {
		return changeRows;
	}

	public void setChangeRows(List<DiffRow> changeRows) {
		this.changeRows = changeRows;
	}
}
