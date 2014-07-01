package org.ihtsdo.buildcloud.service.execution;

import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;

public class ReleaseFileGeneratorFactory {

	public ReleaseFileGenerator createReleaseFileGenerator(Execution execution, Package pkg, ExecutionDAO dao) {
		if (pkg.isFirstTimeRelease()) {
			return new FirstReleaseFileGenerator(execution, pkg, dao);
		} else {
			return new SubsequentReleaseFileGenerator(execution, pkg, dao);
		}
	}

}
