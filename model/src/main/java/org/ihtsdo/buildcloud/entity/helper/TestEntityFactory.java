package org.ihtsdo.buildcloud.entity.helper;

import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Package;

public class TestEntityFactory {

	public Package createPackage(String releaseCentreName, String extensionName, String productName, String releaseName, String packageName) {
		ReleaseCentre releaseCentre = new ReleaseCentre(releaseCentreName);
		Extension extension = new Extension(extensionName);
		Product product = new Product(productName);
		Package aPackage = new Package(packageName);
		Release release = new Release(releaseName);
		release.addPackage(aPackage);
		product.addRelease(release);
		extension.addProduct(product);
		releaseCentre.addExtension(extension);
		return aPackage;
	}

}
