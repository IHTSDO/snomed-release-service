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
													 "NLM Example Refset",
													 "Medical Devices Technical Preview",
													 "GP/FP Refset Technical Preview",
													 "LOINC Expressions Technical Preview",
													 "ICPC2 Map Technical Preview"},

													{"Spanish Release"} };

	public static final int totalBuildCount = 10;
	
	//Number of builds per extension
	public static final int [] buildCount = { 6,
											  4};
	
	//Build array dimensions are extension, products, build

	//So those empty arrays there are for "Medical Devices", et al.
	public static final String [][][] buildNames = { 	{ 	{	"20140731 International Release Build",
																"20140131 International Release Build",
																"20130731 International Release Build",
																"20130131 International Release Build",
																 "Int Daily Build"} , 
					{ "20140831 Simple Refset Build" }, {}, {}, {}, {} },
														{ {	"20140731 Spanish Release Build",
															"20140131 Spanish Release Build",
															"20130731 Spanish Release Build",
															"20130131 Spanish Release Build"}}};

	
	public static final int totalStarredBuilds = 6; 
	
	// Starred count is per extension
	public static final int [] starredCount = { 4,
												2};
	
	public static final boolean[][][] starredBuilds = { { { true, true, true, false, false }, { true }, {}, {}, {}, {} },
															{{ true, false, true, false }} };
	
	//Doing this on a per product basis just like the builds, ie all builds in a product will feature the same package
	public static final String [][][] packageNames = {	{	{	"SNOMED Release Package",
																"RF1 Compatibility Package",
																"RF2 to RF1 Conversion"},
															{	"Simple Refset Package" }, {}, {}, {} },
														{ {	"SNOMED Release Package (es-ar)",
															"RF1 Compatibility Package (es-ar)",
															"RF2 to RF1 Conversion (es-ar)" }}};

	protected ReleaseCenter createTestReleaseCenter(String fullName, String shortName) {
		ReleaseCenter releaseCenter = new ReleaseCenter(fullName, shortName);
		return releaseCenter;
	}

	protected ReleaseCenter createTestReleaseCenterWithExtensions(String fullName, String shortName) {
		ReleaseCenter releaseCenter = createTestReleaseCenter(fullName, shortName);
		addExtensionsToReleaseCenter(releaseCenter);
		return releaseCenter;
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
						int packageNamesAvailable = packageNames[iEx][iPrd].length;

						// Daily Build to only have one package
						if (iEx == 0 && iPrd == 0 && iBld == 4)
							packageNamesAvailable = 1;

						for (int pkgIdx = 0; pkgIdx < packageNamesAvailable; pkgIdx++) {
							Package pkg = new Package(packageNames[iEx][iPrd][pkgIdx]);
							build.addPackage(pkg);
						}
					}
				}
			}
		}
	}
	


}
