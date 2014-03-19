package org.ihtsdo.buildcloud.entity.helper;

import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Package;

import java.util.GregorianCalendar;

public class TestEntityFactory extends TestEntityGenerator {

	public Package createPackage(String releaseCentreName, String releaseCentreShortName, String extensionName, String productName, String buildName, String packageName) {
		ReleaseCentre releaseCentre = new ReleaseCentre(releaseCentreName, releaseCentreShortName);
		Extension extension = new Extension(extensionName);
		Product product = new Product(productName);
		Build build = new Build(1L, buildName);
		Package aPackage = new Package(packageName);
		InputFile inputFile = new InputFile("concepts.rf2", "1.0");
		aPackage.addInputFile(inputFile);
		build.addPackage(aPackage);
		product.addBuild(build);
		extension.addProduct(product);
		releaseCentre.addExtension(extension);
		return aPackage;
	}
	
	public Build createBuild(){
		//TODO Once we have sets, we'll be able to use the test release centre directly, and navigate to find a build
		ReleaseCentre releaseCentre = new ReleaseCentre(releaseCentreNames[0], releaseCentreShortNames[0]);
		Extension extension = new Extension(extensionNames[0]);
		Product product = new Product(productNames[0][0]);
		Build build = new Build(1L,buildNames[0][3]);

		addPackagesToBuild(build);
		product.addBuild(build);
		extension.addProduct(product);
		releaseCentre.addExtension(extension);
		
		return build;
	}

	public Execution createExecution() {
		Build build = createBuild();
		return new Execution(new GregorianCalendar(2013, 2, 5, 16, 30, 00).getTime(), build);
	}

}
