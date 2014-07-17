package org.ihtsdo.buildcloud.service.execution;

import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;

import java.util.Map;

public class ReleaseFileGeneratorFactory {

	public ReleaseFileGenerator createReleaseFileGenerator(Execution execution, Package pkg, Map<String, TableSchema> inputFileSchemaMap, ExecutionDAO dao) {
		if (pkg.isFirstTimeRelease()) {
			return new FirstReleaseFileGenerator(execution, pkg, dao);
		} else {
			return new SubsequentReleaseFileGenerator(execution, pkg, inputFileSchemaMap, dao);
		}
	}

}
