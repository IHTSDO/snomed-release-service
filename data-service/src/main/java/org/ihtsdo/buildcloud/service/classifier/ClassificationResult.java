package org.ihtsdo.buildcloud.service.classifier;

public class ClassificationResult {
	private String resultFilePath;
	private boolean isSnapshotFile;
	public ClassificationResult(String resultFilePath, boolean isSnapshot) {
		this.resultFilePath = resultFilePath;
		this.isSnapshotFile = isSnapshot;
	}
	public String getResultFilePath() {
		return this.resultFilePath;
	}
	public boolean isSnapshot() {
		return this.isSnapshotFile;
	}
}
