package org.ihtsdo.buildcloud.service.execution.transform;

import java.util.List;

public interface BatchLineTransformation extends Transformation {

	void transformLines(List<String[]> columnValues) throws TransformationException;

}
