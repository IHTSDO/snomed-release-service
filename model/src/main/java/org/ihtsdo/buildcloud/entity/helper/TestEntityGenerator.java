package org.ihtsdo.buildcloud.entity.helper;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.InputFile;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.ReleaseCentre;

public class TestEntityGenerator {

	public static final String [] releaseCentreNames = {"International Release Centre"};

	public static final String [] releaseCentreShortNames = {"International"};

	public static final String [] extensionNames = {"SNOMED CT International Edition",
													"SNOMED CT Spanish Edition"};
	
	public static final String [][] productNames = {{"SNOMED CT Release", "Conversion Utility", "Compatibility Package"},
													{"Spanish Release"} };
	
	public static final int totalBuildCount = 8;
	
	public static final int [] buildCount = { 4,
											  4};
	
	//Build array dimensions are extension, products, build
	//So those two empty arrays there are for "Conversion Utility" and "Compatibility Package"
	public static final String [][][] buildNames = { 	{ {	"20140731 International Release",
															"20150131 International Release - Biannual",
															"20150131 International Release - Nightly",
															"20150731 International Release - Biannual"} , {}, {} },
														{ {	"20141031 Spanish Release",
															"20150430 Spanish Release - Semestral",
															"20150430 Spanish Release - Nocturno",
															"20150430 Spanish Release - Semestral"}}};
	
	public static final int totalStarredBuilds = 5; 
	
	// Starred count is per extension
	public static final int [] starredCount = { 3,
												2};
	
	public static final boolean [][][] starredBuilds = {	{{ true, true, true, false  }, {}, {} },
															{{ true, false, true, false }} };
	
	public static final String [] packageNames = {	"RF2 Release",
													"RF1CompatibilityPackage",
													"RF2toRF1Conversion"};		
	
	public static final String [] inputFileNames = { "concepts.rf2" };
	
	protected ReleaseCentre createTestReleaseCentre() {
		
		ReleaseCentre internationalReleaseCentre = new ReleaseCentre(releaseCentreNames[0], releaseCentreShortNames[0]);
		addExtensionsToReleaseCentre(internationalReleaseCentre);	
		return internationalReleaseCentre;
	}	
	

	protected void addExtensionsToReleaseCentre (ReleaseCentre releaseCentre) {
		for (int iEx=0; iEx < extensionNames.length; iEx++){
			Extension extension = new Extension (extensionNames[iEx]);
			releaseCentre.addExtension(extension);
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
		for (String packageName: packageNames) {
			Package pkg = new Package(packageName);
			build.addPackage(pkg);
			if (packageName.equals(packageNames[0])){
				InputFile inputFile = new InputFile("concepts rf2", "2014-03-19T14-14-48");
				inputFile.setGroupId("org.ihtsdo.release.international.snomed_ct_international_edition.snomed_ct_international_edition.20140731_international_release");
				inputFile.setArtifactId("rf2_release.input.concepts_rf2");
				pkg.addInputFile(inputFile);
			}
		}		
	}	
	
}
