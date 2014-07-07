package org.ihtsdo.buildcloud.service.precondition;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.ihtsdo.buildcloud.dao.DAOFactory;
import org.ihtsdo.buildcloud.dao.InputFileDAO;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ManifestCheckTest extends PreconditionCheckTest{
	
	@Before
	public void setup() {
		super.setup();
		manager = PreconditionManager.build(execution)
				.add(new ManifestCheck());
	}

	@Test
	public void checkNoManifest() throws FileNotFoundException, InstantiationException, IllegalAccessException {
		loadManifest(null);
		String actualResult = runPreConditionCheck(ManifestCheck.class);
		Assert.assertEquals( PreconditionCheck.State.FAIL.toString(), actualResult);
	}

	@Test
	public void checkValidManifest() throws FileNotFoundException, InstantiationException, IllegalAccessException {
		loadManifest("valid_manifest.xml");
		String actualResult = runPreConditionCheck(ManifestCheck.class);
		Assert.assertEquals( PreconditionCheck.State.PASS.toString(), actualResult);
	}
	
	@Test
	public void checkInvalidManifest() throws FileNotFoundException, InstantiationException, IllegalAccessException {
		loadManifest("invalid_manifest.xml");
		String actualResult = runPreConditionCheck(ManifestCheck.class);
		Assert.assertEquals( PreconditionCheck.State.FAIL.toString(), actualResult);
	}
	
	@Test
	public void checkNoNamespaceManifest() throws FileNotFoundException, InstantiationException, IllegalAccessException {
		loadManifest("no_namespace_otherwise_valid_manifest.xml");
		String actualResult = runPreConditionCheck(ManifestCheck.class);
		Assert.assertEquals( PreconditionCheck.State.FAIL.toString(), actualResult);
	}
	
	private void loadManifest(String filename) throws FileNotFoundException {
		InputFileDAO dao = DAOFactory.getInputFileDAO();
		for (Package pkg : build.getPackages()) {
			if (filename != null) {
				String testFilePath = getClass().getResource(filename).getFile();
				File testManifest = new File (testFilePath);
				dao.putManifestFile(pkg, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
			} else {
				dao.deleteManifest(pkg);
			}
		}
		
		//When we load a manifest, we need that copied over to a new execution
		createNewExecution();
	}

}
