package org.ihtsdo.buildcloud.entity.helper;

import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Package;

public class TestEntityFactory {

	public Package createPackage(String releaseCentreName, String extensionName, String productName, String buildName, String packageName) {
		ReleaseCentre releaseCentre = new ReleaseCentre(releaseCentreName);
		Extension extension = new Extension(extensionName);
		Product product = new Product(productName);
		Build build = new Build(buildName);
		Package aPackage = new Package(packageName);
		InputFile inputFile = new InputFile("concepts.rf2");
		aPackage.addInputFile(inputFile);
		build.addPackage(aPackage);
		product.addBuild(build);
		extension.addProduct(product);
		releaseCentre.addExtension(extension);
		return aPackage;
	}

}
