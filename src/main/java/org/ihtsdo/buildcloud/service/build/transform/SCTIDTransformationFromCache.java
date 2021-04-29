package org.ihtsdo.buildcloud.service.build.transform;

public class SCTIDTransformationFromCache implements LineTransformation {

	private final CachedSctidFactory sctidFactory;

	private final int sctIdCol;

	public SCTIDTransformationFromCache(int sctIdCol, CachedSctidFactory sctidFactory) {
		this.sctidFactory = sctidFactory;
		this.sctIdCol = sctIdCol;
	}

	@Override
	public void transformLine(String[] columnValues) throws TransformationException {
		if (columnValues != null && columnValues.length > sctIdCol && columnValues[sctIdCol].contains("-")) {
			// Value is temp UUID from authoring tool.
			// Replace with SCTID.
			String uuidString = columnValues[sctIdCol];
			Long sctid = sctidFactory.getSCTIDFromCache(uuidString);
			if (sctid != null) {
				columnValues[sctIdCol] = sctid.toString();
			} else {
				throw new TransformationException("SCTID does not exist in cache for UUID '" + uuidString + "'");
			}
		}
	}

	@Override
	public int getColumnIndex() {
		return sctIdCol;
	}

}
