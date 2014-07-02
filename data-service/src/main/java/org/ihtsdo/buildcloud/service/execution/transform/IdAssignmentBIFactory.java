package org.ihtsdo.buildcloud.service.execution.transform;

import org.ihtsdo.idgeneration.IdAssignmentBI;

public class IdAssignmentBIFactory {

	private IdAssignmentBI onlineImplementation;
	private IdAssignmentBI offlineImplementation;

	public IdAssignmentBIFactory(IdAssignmentBI onlineImplementation, IdAssignmentBI offlineImplementation) {
		this.onlineImplementation = onlineImplementation;
		this.offlineImplementation = offlineImplementation;
	}

	public IdAssignmentBI getInstance(boolean offlineMode) {
		if (offlineMode) {
			return offlineImplementation;
		} else {
			return onlineImplementation;
		}
	}

}
