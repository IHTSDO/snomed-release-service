package org.ihtsdo.buildcloud.core.service.build.transform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SCTIDTransformation implements BatchLineTransformation {

	public static final String ID_GEN_MODULE_ID_PARAM = "1";
	
	private final CachedSctidFactory sctidFactory;

	private final int componentIdCol;
	private final int moduleIdCol;
	private final String partitionId;

	public SCTIDTransformation(int componentIdCol, int moduleIdCol, String partitionId, CachedSctidFactory sctidFactory) {
		this.sctidFactory = sctidFactory;
		this.componentIdCol = componentIdCol;
		this.moduleIdCol = moduleIdCol;
		this.partitionId = partitionId;
	}

	public void transformLine(String[] columnValues) throws TransformationException {
		if (columnValues != null && columnValues.length > componentIdCol &&
				(columnValues[componentIdCol].contains("-"))) {

			String uuidString = columnValues[componentIdCol];
			// Replace with SCTID.
			try {
				String moduleId = columnValues[moduleIdCol];

				Long sctid = sctidFactory.getSCTID(uuidString, partitionId, moduleId);

				columnValues[componentIdCol] = sctid.toString();
			} catch (Exception e) {
				throw new TransformationException("SCTID creation request failed.", e);
			}
		}
	}

	@Override
	public void transformLines(List<String[]> columnValuesList) throws TransformationException {
		// Collect uuid strings
		List<String> uuidStrings = new ArrayList<>();
		String uuidString;
		for (String[] columnValues : columnValuesList) {
			uuidString = columnValues[componentIdCol];
			if (uuidString.contains("-")) {
				uuidStrings.add(uuidString);
			}
		}
		transformLineGroup(columnValuesList, uuidStrings);
	}

	public void transformLineGroup(List<String[]> columnValuesList, List<String> uuidStrings) throws TransformationException {
		try {
			Map<String, Long> sctiDs = sctidFactory.getSCTIDs(uuidStrings, partitionId, ID_GEN_MODULE_ID_PARAM);
			// Replace UUIDs with looked up SCTIDs
			for (String[] columnValuesReplace : columnValuesList) {
				String idString = columnValuesReplace[componentIdCol];
				if (idString.contains("-")) {
					Long aLong = sctiDs.get(idString);
					if (aLong != null) {
						columnValuesReplace[componentIdCol] = aLong.toString();
					} else {
						throw new TransformationException("No SCTID for UUID " + idString);
					}
				}
			}
		} catch (Exception e) {
			
			throw new TransformationException("SCTID list creation request failed due to:" + e.getMessage(), e);
		}
	}

}
