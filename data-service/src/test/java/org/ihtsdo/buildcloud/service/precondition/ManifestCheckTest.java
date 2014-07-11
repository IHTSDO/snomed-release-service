package org.ihtsdo.buildcloud.service.precondition;

import java.io.FileNotFoundException;

import org.ihtsdo.buildcloud.dao.InputFileDAO;
import org.ihtsdo.buildcloud.entity.PreConditionCheckReport.State;
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
		State actualResult = runPreConditionCheck(ManifestCheck.class);
		Assert.assertEquals( State.FAIL, actualResult);
	}

	@Test
	public void checkValidManifest() throws FileNotFoundException, InstantiationException, IllegalAccessException {
		loadManifest("valid_manifest.xml");
		State actualResult = runPreConditionCheck(ManifestCheck.class);
		Assert.assertEquals(State.PASS, actualResult);
	}

	@Test
	public void checkInvalidManifest() throws FileNotFoundException, InstantiationException, IllegalAccessException {
		loadManifest("invalid_manifest.xml");
		State actualResult = runPreConditionCheck(ManifestCheck.class);
		Assert.assertEquals( State.FAIL, actualResult);
	}

	@Test
	public void checkNoNamespaceManifest() throws FileNotFoundException, InstantiationException, IllegalAccessException {
		loadManifest("no_namespace_otherwise_valid_manifest.xml");
		State actualResult = runPreConditionCheck(ManifestCheck.class);
		Assert.assertEquals( State.FAIL, actualResult);
	}

}
