package org.ihtsdo.buildcloud.service.srs;

import java.util.Arrays;

public class RefsetCombiner {

	String targetFilePattern;
	String[] sourceFilePatterns;

	public RefsetCombiner(String targetFilePattern, String[] sourceFilePatterns) {
		this.targetFilePattern = targetFilePattern;
		this.sourceFilePatterns = sourceFilePatterns;
	}

	@Override
	public String toString() {
		return "RefsetCombiner [targetFilePattern=" + targetFilePattern
				+ ", sourceFilePatterns=" + Arrays.toString(sourceFilePatterns)
				+ "]";
	}
}
