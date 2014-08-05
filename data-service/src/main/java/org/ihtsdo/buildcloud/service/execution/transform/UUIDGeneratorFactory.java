package org.ihtsdo.buildcloud.service.execution.transform;

public class UUIDGeneratorFactory {

	UUIDGenerator randomGenerator;
	UUIDGenerator pseudoGenerator;


	public UUIDGeneratorFactory(UUIDGenerator randomGenerator, UUIDGenerator pseudoGenerator) {
		this.randomGenerator = randomGenerator;
		this.pseudoGenerator = pseudoGenerator;
	}

	public UUIDGenerator getInstance(boolean isOffLine) {
		if (isOffLine) {
			return pseudoGenerator;
		}
		return randomGenerator;
	}

}
