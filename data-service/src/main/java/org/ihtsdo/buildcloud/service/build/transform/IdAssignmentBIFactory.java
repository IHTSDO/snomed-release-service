package org.ihtsdo.buildcloud.service.build.transform;

import org.ihtsdo.idgeneration.IdAssignmentBI;

public class IdAssignmentBIFactory {

	private final IdAssignmentBI onlineImplementation;
	private final IdAssignmentBI offlineImplementation;

	public IdAssignmentBIFactory(final IdAssignmentBI onlineImplementation, final IdAssignmentBI offlineImplementation) {
		this.onlineImplementation = onlineImplementation;
		this.offlineImplementation = offlineImplementation;
	}

	public IdAssignmentBI getInstance(final boolean offlineMode) {
		if (offlineMode) {
			return offlineImplementation;
		} else {
			return onlineImplementation;
		}
	}
}
