package org.ihtsdo.buildcloud.service.precondition;

import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.service.execution.Zipper;
import org.springframework.beans.factory.annotation.Autowired;

public class ManifestCheck extends PreconditionCheck {

	@Autowired
	private ExecutionDAO executionDAO;

	@Override
	public void runCheck(Package pkg, Execution execution) {

		//Firstly, check that a manifest file is present.  Actually, Zipper will check that for us		
		//Now check that the manifest conforms to the XSD and specifically, that we can find a valid root folder
		//Again, zipper will check that as it loads the manifest
		try {
			Zipper zipper = new Zipper(execution, pkg, executionDAO);
			zipper.loadManifest();
			pass();
		} catch (Exception e) {
			fatalError("Package manifest is missing or invalid: " + e.getMessage());
		}
	}

}
