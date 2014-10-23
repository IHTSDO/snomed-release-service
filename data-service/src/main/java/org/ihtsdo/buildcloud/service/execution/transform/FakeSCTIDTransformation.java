package org.ihtsdo.buildcloud.service.execution.transform;

public class FakeSCTIDTransformation implements LineTransformation {

	private final int componentIdCol;
	private long fakeSctId = 800011021;

	public FakeSCTIDTransformation(int componentIdCol) {
		this.componentIdCol = componentIdCol;
	}

	public void transformLine(String[] columnValues) throws TransformationException {
		if (columnValues != null && columnValues.length > componentIdCol &&
				(columnValues[componentIdCol].contains("-") || columnValues[componentIdCol].equals("null"))) {

			Long sctId = fakeSctId++;
			columnValues[componentIdCol] = sctId.toString();
		}
	}

	@Override
	public int getColumnIndex() {
		return componentIdCol;
	}

}
