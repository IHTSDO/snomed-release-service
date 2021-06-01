package org.ihtsdo.buildcloud.core.service.build.transform;

import java.util.Map;

public class ReplaceStringTransform implements LineTransformation {

	private final int columnIndex;
	private final Map<String, String> matchToReplacementMap;

	public ReplaceStringTransform(int columnIndex, Map<String, String> matchToReplacementMap) {
		this.columnIndex = columnIndex;
		this.matchToReplacementMap = matchToReplacementMap;
	}

	@Override
	public void transformLine(String[] columnValues) throws TransformationException {
		String existing = columnValues[columnIndex];
		if (matchToReplacementMap.containsKey(existing)) {
			columnValues[columnIndex] = matchToReplacementMap.get(existing);
		}
	}

	@Override
	public int getColumnIndex() {
		return columnIndex;
	}

}
