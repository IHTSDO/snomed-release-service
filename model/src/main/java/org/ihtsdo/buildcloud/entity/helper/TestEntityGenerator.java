package org.ihtsdo.buildcloud.entity.helper;

import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Package;

public class TestEntityGenerator {
	
	public static final User TEST_USER = new User(1L, "test");

	public static final String [] releaseCenterNames = {"International Release Center"};

	public static final String [] releaseCenterShortNames = {"International"};

	public static final String [] extensionNames = {"SNOMED CT International Edition",
													"SNOMED CT Spanish Edition"
													};

	public static final String [][] productNames = {{"SNOMED CT Release",
													 "SNOMED Special Refset Preview",
													 "Medical Devices Technical Preview",
													 "GP/FP Refset Technical Preview",
													 "LOINC Expressions Technical Preview",
													 "ICPC2 Map Technical Preview"},

													{"Spanish Release"} };

	public static final int totalBuildCount = 9;
	
	//Number of builds per extension
	public static final int [] buildCount = { 5,
											  4};
	
	//Build array dimensions are extension, products, build

	//So those empty arrays there are for "Medical Devices", et al.
	public static final String [][][] buildNames = { 	{ 	{	"20140731 International Release Build",
																"20140131 International Release Build",
																"20130731 International Release Build",
																"20130131 International Release Build"} , 
															{"20140831 Simple Refset Build"}, {}, {}, {}, {} },
														{ {	"20140731 Spanish Release Build",
															"20140131 Spanish Release Build",
															"20130731 Spanish Release Build",
															"20130131 Spanish Release Build"}}};

	
	public static final int totalStarredBuilds = 6; 
	
	// Starred count is per extension
	public static final int [] starredCount = { 4,
												2};
	
	public static final boolean [][][] starredBuilds = {	{{ true, true, true, false  }, {true}, {}, {}, {}, {} },

															{{ true, false, true, false }} };
	
	//Doing this on a per product basis just like the builds, ie all builds in a product will feature the same package
	public static final String [][][] packageNames = {	{	{	"SNOMED Release Package",
																"RF1 Compatibility Package",
																"RF2 to RF1 Conversion"},
															{	"Simple Refset Package" }, {}, {}, {} },
														{ {	"SNOMED Release Package (es-ar)",
															"RF1 Compatibility Package (es-ar)",
															"RF2 to RF1 Conversion (es-ar)" }}};

	// packageInputFile string format is name|groupId|artifactId|version
	public static final String [] packageInputFiles = {
			"concepts rf2|org.ihtsdo.release.international.snomed_ct_international_edition.snomed_ct_international_edition.20140731_international_release|rf2_release.input.concepts_rf2|2014-03-19T14-14-48",
			"RF1CompatibilityPackage|org.ihtsdo.release.international.snomed_ct_international_edition.snomed_ct_international_edition.20140731_international_release|rf1compatibilitypackage.input.rf1compatibilitypackage|2014-03-27T08-44-32",
			"RF2_RF1_Converter|org.ihtsdo.release.international.snomed_ct_international_edition.snomed_ct_international_edition.20140731_international_release|rf2torf1conversion.input.rf2_rf1_converter|2014-03-27T08-45-12"
	};
	
	public static final String manifestFileStr = "SnomedCT_Release_INT_20140131.xml|org.ihtsdo.release.international.snomed_ct_international_edition.snomed_ct_release.20140731_international_release_build|snomed_release_package.input.snomedct_release_int_20140131xml|2014-05-15T15-29-58";
	
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
					
					//Do we have packages to add to all builds of this product?
					if (packageNames.length > iEx && packageNames[iEx].length > iPrd){
						for (int pkgIdx = 0; pkgIdx < packageNames[iEx][iPrd].length; pkgIdx++) {
							Package pkg = new Package(packageNames[iEx][iPrd][pkgIdx]);
							build.addPackage(pkg);
						}
					}
				}
			}
		}
	}
	


}
