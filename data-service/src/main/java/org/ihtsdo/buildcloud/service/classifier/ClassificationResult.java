package org.ihtsdo.buildcloud.service.classifier;

public class ClassificationResult {
	
	private String resultFilename;
	private boolean isSnapshotFile;
	private String extraResultFileName;
	
	public ClassificationResult(String resultFilename, boolean isSnapshot) {
		this.resultFilename = resultFilename;
		this.isSnapshotFile = isSnapshot;
	}
	
	public String getResultFilename() {
		return this.resultFilename;
	}
	
	public boolean isSnapshot() {
		return this.isSnapshotFile;
	}

	public String getExtraResultFileName() {
		return extraResultFileName;
	}

	public void setExtraResultFileName(String extraResultFileName) {
		this.extraResultFileName = extraResultFileName;
	}

	public void setResultFilename(String resultFilename) {
		this.resultFilename = resultFilename;
	}

	@Override
	public String toString() {
		return "ClassificationResult [resultFilename=" + resultFilename + ", isSnapshotFile=" + isSnapshotFile
				+ ", extraResultFileName=" + extraResultFileName + "]";
	}
	
}
