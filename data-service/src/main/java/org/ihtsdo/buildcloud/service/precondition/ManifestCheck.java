package org.ihtsdo.buildcloud.service.precondition;

import org.ihtsdo.buildcloud.dao.DAOFactory;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.service.execution.Zipper;

public class ManifestCheck extends PreconditionCheck {

	@Override
	public void runCheck(Package pkg) {

		//Firstly, check that a manifest file is present.  Actually, Zipper will check that for us		
		//Now check that the manifest conforms to the XSD and specifically, that we can find a valid root folder
		//Again, zipper will check that as it loads the manifest
		try {
			Zipper zipper = new Zipper(getExecution(), pkg, DAOFactory.getExecutionDAO());
			zipper.loadManifest();
			pass();
		} catch (Exception e) {
			fail ("Package manifest is missing or invalid: " + e.getMessage());
		}
	}

}
