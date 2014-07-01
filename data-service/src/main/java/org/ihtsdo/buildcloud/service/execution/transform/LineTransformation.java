package org.ihtsdo.buildcloud.service.execution.transform;

public interface LineTransformation {

	void transformLine(String[] columnValues) throws TransformationException;

}
