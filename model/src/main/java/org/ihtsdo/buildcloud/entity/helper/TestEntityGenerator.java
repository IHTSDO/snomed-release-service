package org.ihtsdo.buildcloud.entity.helper;

import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;

public class TestEntityGenerator {
	
	public static final String TEST_USER = "test";

	public static final String [] releaseCenterNames = {"International Release Center"};

	public static final String [] releaseCenterShortNames = {"International"};

	public static final String [] extensionNames = {"SNOMED CT International Edition",
													"SNOMED CT Spanish Edition"};
	
	public static final String [][] productNames = {{"SNOMED CT Release", 
													 "Medical Devices Technical Preview",
													 "GP/FP Refset Technical Preview",
													 "LOINC Expressions Technical Preview",
													 "ICPC2 Map Technical Preview"},
													{"Spanish Release"} };
	
	public static final int totalBuildCount = 8;
	
	public static final int [] buildCount = { 4,
											  4};
	
	//Build array dimensions are extension, products, build
	//So those empty arrays there are for "Medical Devices", et al.
	public static final String [][][] buildNames = { 	{ {	"20140731 International Release Build",
															"20140131 International Release Build",
															"20130731 International Release Build",
															"20130131 International Release Build"} , {}, {}, {}, {} },
														{ {	"20140731 Spanish Release Build",
															"20140131 Spanish Release Build",
															"20130731 Spanish Release Build",
															"20130131 Spanish Release Build"}}};
	
	public static final int totalStarredBuilds = 5; 
	
	// Starred count is per extension
	public static final int [] starredCount = { 3,
												2};
	
	public static final boolean [][][] starredBuilds = {	{{ true, true, true, false  }, {}, {}, {}, {} },
															{{ true, false, true, false }} };
	
	public static final String [] packageNames = {	"RF2 Release",
													"RF1CompatibilityPackage",
													"RF2toRF1Conversion"};		

	// packageInputFile string format is name|groupId|artifactId|version
	public static final String [] packageInputFiles = {
			"concepts rf2|org.ihtsdo.release.international.snomed_ct_international_edition.snomed_ct_international_edition.20140731_international_release|rf2_release.input.concepts_rf2|2014-03-19T14-14-48",
			"RF1CompatibilityPackage|org.ihtsdo.release.international.snomed_ct_international_edition.snomed_ct_international_edition.20140731_international_release|rf1compatibilitypackage.input.rf1compatibilitypackage|2014-03-27T08-44-32",
			"RF2_RF1_Converter|org.ihtsdo.release.international.snomed_ct_international_edition.snomed_ct_international_edition.20140731_international_release|rf2torf1conversion.input.rf2_rf1_converter|2014-03-27T08-45-12"
	};
	
	protected ReleaseCenter createTestReleaseCenter() {
		
		ReleaseCenter internationalReleaseCenter = new ReleaseCenter(releaseCenterNames[0], releaseCenterShortNames[0]);
		addExtensionsToReleaseCenter(internationalReleaseCenter);
		return internationalReleaseCenter;
	}	
	

	protected void addExtensionsToReleaseCenter (ReleaseCenter releaseCenter) {
		for (int iEx=0; iEx < extensionNames.length; iEx++){
			Extension extension = new Extension (extensionNames[iEx]);
			releaseCenter.addExtension(extension);
			for (int iPrd=0; iPrd < productNames[iEx].length; iPrd++) {
				Product product = new Product (productNames[iEx][iPrd]);
				extension.addProduct(product);
				for (int iBld=0; iBld < buildNames[iEx][iPrd].length; iBld++){
					Build build = new Build(buildNames[iEx][iPrd][iBld], starredBuilds[iEx][iPrd][iBld]);
					product.addBuild(build);
					addPackagesToBuild(build);
				}
			}
		}
	}
	
	protected void addPackagesToBuild (Build build) {
		for (int i = 0; i < packageNames.length; i++) {
			String packageName = packageNames[i];
			Package pkg = new Package(packageName);
			build.addPackage(pkg);
			if (packageInputFiles.length > i) {
				String[] inputFileParts = packageInputFiles[i].split("\\|");
				InputFile inputFile = new InputFile(inputFileParts[0], inputFileParts[3]);
				inputFile.setGroupId(inputFileParts[1]);
				inputFile.setArtifactId(inputFileParts[2]);
				pkg.addInputFile(inputFile);
			}
		}		
	}	
	
}
