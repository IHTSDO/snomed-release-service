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
	public final void setup() throws Exception {
		super.setup();
		manager = new PreconditionManager().preconditionChecks(manifestCheck);
	}

	@Test
	public final void checkNoManifest() throws FileNotFoundException, InstantiationException, IllegalAccessException {
		loadManifest(null);
		State actualResult = runPreConditionCheck(ManifestCheck.class).getResult();
		Assert.assertEquals(State.FATAL, actualResult);
	}

	@Test
	public final void checkValidManifest() throws FileNotFoundException, InstantiationException, IllegalAccessException {
		loadManifest("valid_manifest.xml");
		State actualResult = runPreConditionCheck(ManifestCheck.class).getResult();
		Assert.assertEquals(State.PASS, actualResult);
	}

	@Test
	public final void checkInvalidManifest() throws FileNotFoundException, InstantiationException, IllegalAccessException {
		loadManifest("invalid_manifest.xml");
		State actualResult = runPreConditionCheck(ManifestCheck.class).getResult();
		Assert.assertEquals(State.FATAL, actualResult);
	}

	@Test
	public final void checkNoNamespaceManifest() throws FileNotFoundException, InstantiationException, IllegalAccessException {
		loadManifest("no_namespace_otherwise_valid_manifest.xml");
		State actualResult = runPreConditionCheck(ManifestCheck.class).getResult();
		Assert.assertEquals(State.FATAL, actualResult);
	}

}
