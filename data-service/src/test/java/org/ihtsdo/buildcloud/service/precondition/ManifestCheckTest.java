package org.ihtsdo.buildcloud.service.precondition;

import org.ihtsdo.buildcloud.entity.PreConditionCheckReport;
import org.ihtsdo.buildcloud.entity.PreConditionCheckReport.State;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ManifestCheckTest extends PreconditionCheckTest {

	@Autowired
	private ManifestCheck manifestCheck;

	@Override
	@Before
	public final void setup() throws Exception {
		super.setup();
		manager = new PreconditionManager().preconditionChecks(manifestCheck);
	}

	@Test
	public final void checkNoManifest() throws Exception {
		loadManifest(null);
		final State actualResult = runPreConditionCheck(ManifestCheck.class).getResult();
		Assert.assertEquals(State.FATAL, actualResult);
	}

	@Test
	public final void checkValidManifest() throws Exception {
		loadManifest("valid_manifest.xml");
		final State actualResult = runPreConditionCheck(ManifestCheck.class).getResult();
		Assert.assertEquals(State.PASS, actualResult);
	}

	@Test
	public final void checkInvalidManifest() throws Exception {
		loadManifest("invalid_manifest.xml");
		final State actualResult = runPreConditionCheck(ManifestCheck.class).getResult();
		Assert.assertEquals(State.FATAL, actualResult);
	}

	@Test
	@Ignore
	public final void checkNoNamespaceManifest() throws Exception {
		loadManifest("no_namespace_otherwise_valid_manifest.xml");
		final State actualResult = runPreConditionCheck(ManifestCheck.class).getResult();
		Assert.assertEquals(State.FATAL, actualResult);
	}
	
	@Test
	public final void checkManifestWithNotMatchedDate() throws Exception {
		loadManifest("august_release_manifest.xml");
		final PreConditionCheckReport report = runPreConditionCheck(ManifestCheck.class);
		final State actualResult = report.getResult();
		Assert.assertEquals(State.WARNING, actualResult);
		final String expectedMsg = "The following file names specified in the manifest:SnomedCT_ReleaseRF2_PRODUCTION_20140831T120000Z,Readme_en_20140831.txt,der2_Refset_SimpleFull_INT_20140831.txt,"
				+ "der2_Refset_SimpleSnapshot_INT_20140831.txt,der2_Refset_SimpleDelta_INT_20140831.txt don't have the same release date as configured in the product:20140731.";
		Assert.assertEquals(expectedMsg, report.getMessage());
	}
	
	@Test
	public final void checkFileNameFormatSpecifedInManifest() throws Exception {
		loadManifest("containing_invalid_fileName_manifest.xml");
		final PreConditionCheckReport report = runPreConditionCheck(ManifestCheck.class);
		final State actualResult = report.getResult();
		Assert.assertEquals(State.FAIL, actualResult);
		final String expectedMsg = "The following file names specified in the manifest:der2_cRefset_LanguageFull-en_20140731.txt,sct2_Extra_Concept_Full_INT_20140731.txt,sct2-TextDefinition-Full-en-INT-20140731.txt "
				+ "don't follow naming convention:<FileType>_<ContentType>_<ContentSubType>_<Country|Namespace>_<VersionDate>.<Extension>.";

		Assert.assertEquals(expectedMsg, report.getMessage());
	}
	
	@Test
	public void checkNoReadmeFoundInManifest() throws Exception{
		loadManifest("no_readme_manifest.xml");
		final PreConditionCheckReport report = runPreConditionCheck(ManifestCheck.class);
		final State actualResult = report.getResult();
		Assert.assertEquals(State.FAIL, actualResult);
		final String expectedMsg = "No Readme file ends with .txt found in manifest.";
		Assert.assertEquals(expectedMsg, report.getMessage());
	}
	
	@Test
	public void checkReleaseDateWithRF1Files() throws Exception {
		loadManifest("manifest_with_rf1_wrong_release_date.xml");
		final PreConditionCheckReport report = runPreConditionCheck(ManifestCheck.class);
		final State actualResult = report.getResult();
		Assert.assertEquals(State.WARNING, actualResult);
		final String expectedMsg = "The following file names specified in the manifest:der1_SubsetMembers_es_INT_20140131.txt,der1_Subsets_es_INT_20140131.txt,sct1_Concepts_Core_INT_20140131.txt "
				+ "don't have the same release date as configured in the product:20140731.";
		Assert.assertEquals(expectedMsg, report.getMessage());
	}

	@Test
	public void checkInvalidReleasePackageName() throws Exception {
		loadManifest("manifest_with_invalid_release_package_name.xml");
		final PreConditionCheckReport report = runPreConditionCheck(ManifestCheck.class);
		final State actualResult = report.getResult();
		Assert.assertEquals(State.WARNING, actualResult);
		final String expectedMsg = "The package name does not follow the packaging conventions: [x prefix if applicable]SnomedCT_[Product][Format(optional)]_[ReleaseStatus]_[Releasedate]T[Releasetime]Z";
		Assert.assertEquals(expectedMsg, report.getMessage());
	}

	@Test
	public void checkInvalidFileNameAgainstBetaRelease() throws Exception {
		final boolean isBetaRelease = true;
		loadManifest("manifest_with_invalid_file_name_against_beta_release.xml", isBetaRelease);
		final PreConditionCheckReport report = runPreConditionCheck(ManifestCheck.class);
		final State actualResult = report.getResult();
		Assert.assertEquals(State.FATAL, actualResult);
		final String expectedMsg = "The following files which specified in the manifest are required starting with x for a Beta release: SnomedCT_ReleaseRF2_20140731T120000Z,der2_Refset_SimpleFull_INT_20140731.txt,der2_Refset_SimpleSnapshot_INT_20140731.txt,der2_Refset_SimpleDelta_INT_20140731.txt";
		Assert.assertEquals(expectedMsg, report.getMessage());
	}
	
	@Test
	public void checkNoFileNamesSpecified() throws Exception {
		loadManifest("manifest_without_fileName.xml");
		final PreConditionCheckReport report = runPreConditionCheck(ManifestCheck.class);
		final State actualResult = report.getResult();
		Assert.assertEquals(State.FAIL, actualResult);
		final String expectedMsg = "Total number of files with no file name specified: 3";
		Assert.assertEquals(expectedMsg, report.getMessage());
	}

}
