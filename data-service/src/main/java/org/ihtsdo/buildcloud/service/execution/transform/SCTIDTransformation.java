package org.ihtsdo.buildcloud.service.execution.transform;

import org.ihtsdo.idgen.ws.CreateSCTIDFaultException;
import org.ihtsdo.idgen.ws.CreateSCTIDListFaultException;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SCTIDTransformation implements BatchLineTransformation {

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
		if (columnValues != null && columnValues.length > componentIdCol && columnValues[componentIdCol].contains("-")) {
			// Value is temp UUID from authoring tool.
			// Replace with SCTID.
			try {
				String uuidString = columnValues[componentIdCol];
				String moduleId = columnValues[moduleIdCol];

				Long sctid = sctidFactory.getSCTID(uuidString, partitionId, moduleId);

				columnValues[componentIdCol] = sctid.toString();
			} catch (CreateSCTIDFaultException | RemoteException | InterruptedException e) {
				throw new TransformationException("SCTID creation request failed.", e);
			}
		}
	}

	@Override
	public void transformLines(List<String[]> columnValuesList) throws TransformationException {
		// Collect uuid strings grouped by moduleId
		List<String> uuidStrings = new ArrayList<>();
		String idString, moduleId, groupModuleId = null;
		int runStart = 0, runEnd = 0;
		for (String[] columnValues : columnValuesList) {
			idString = columnValues[componentIdCol];
			if (idString.contains("-")) {
				moduleId = columnValues[moduleIdCol];
				if (!moduleId.equals(groupModuleId)) {
					if (groupModuleId != null) {
						// Process collected UUIDs for this moduleId run.
						// ID Gen/Lookup for UUID-moduleId group
						transformLineGroup(columnValuesList, uuidStrings, groupModuleId, runStart, runEnd);
					}
					uuidStrings.clear();
					groupModuleId = moduleId;
					runStart = runEnd;
				}
				uuidStrings.add(idString);
			}
			runEnd++;
		}

		if (!uuidStrings.isEmpty() && groupModuleId != null) {
			transformLineGroup(columnValuesList, uuidStrings, groupModuleId, runStart, runEnd);
		}

	}

	public void transformLineGroup(List<String[]> columnValuesList, List<String> uuidStrings, String groupModuleId,
			int runStart, int runEnd) throws TransformationException {

		try {
			Map<String, Long> sctiDs = sctidFactory.getSCTIDs(uuidStrings, partitionId, groupModuleId);

			// Replace UUIDs with looked up SCTIDs
			for (int a = runStart; a < runEnd; a++) {
				String[] columnValuesReplace = columnValuesList.get(a);
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

		} catch (RemoteException | CreateSCTIDListFaultException | InterruptedException e) {
			throw new TransformationException("SCTID list creation request failed.", e);
		}
	}

}
