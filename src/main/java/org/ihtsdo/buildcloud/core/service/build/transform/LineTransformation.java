package org.ihtsdo.buildcloud.core.service.build.transform;

public interface LineTransformation extends Transformation {

	void transformLine(String[] columnValues) throws TransformationException;

	int getColumnIndex();

}
