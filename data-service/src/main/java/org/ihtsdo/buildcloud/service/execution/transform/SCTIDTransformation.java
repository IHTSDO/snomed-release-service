package org.ihtsdo.buildcloud.service.execution.transform;

import org.ihtsdo.idgen.ws.CreateSCTIDFaultException;

import java.rmi.RemoteException;

public class SCTIDTransformation implements LineTransformation {

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

	@Override
	public void transformLine(String[] columnValues) throws TransformationException {
		if (columnValues[componentIdCol].contains("-")) {
			// Value is temp UUID from authoring tool.
			// Replace with SCTID.
			try {
				String uuidString = columnValues[componentIdCol];
				String moduleId = columnValues[moduleIdCol];

				Long sctid = sctidFactory.getSCTID(uuidString, partitionId, moduleId);

				columnValues[componentIdCol] = sctid.toString();
			} catch (CreateSCTIDFaultException | RemoteException e) {
				throw new TransformationException("SCTID creation request failed.", e);
			}
		}
	}

	@Override
	public int getColumnIndex() {
		return componentIdCol;
	}

}
