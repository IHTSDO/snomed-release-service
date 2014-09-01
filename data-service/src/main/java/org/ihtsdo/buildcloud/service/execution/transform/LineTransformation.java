package org.ihtsdo.buildcloud.service.execution.transform;

public interface LineTransformation extends Transformation {

	void transformLine(String[] columnValues) throws TransformationException;

	int getColumnIndex();

}
