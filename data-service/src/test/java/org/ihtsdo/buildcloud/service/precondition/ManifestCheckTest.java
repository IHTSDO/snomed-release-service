package org.ihtsdo.buildcloud.service.precondition;

import java.io.FileNotFoundException;

import org.ihtsdo.buildcloud.dao.InputFileDAO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ManifestCheckTest extends PreconditionCheckTest {

	@Autowired
	private InputFileDAO inputFileDAO;

	@Autowired
	private ManifestCheck manifestCheck;

	@Override
	@Before
	public void setup() {
		super.setup();
		manager = new PreconditionManager().preconditionChecks(manifestCheck);
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
		Assert.assertEquals(PreconditionCheck.State.PASS.toString(), actualResult);
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

}
