package org.ihtsdo.buildcloud.service.build.transform;

import java.util.List;

public interface BatchLineTransformation extends Transformation {

	void transformLines(List<String[]> columnValues) throws TransformationException;

}
