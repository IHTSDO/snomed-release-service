package org.ihtsdo.buildcloud.entity.helper;

import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.ReleaseCentre;

public class TestEntityFactory {

	public Package createPackage(String releaseCentreName, String extensionName, String productName, String packageName) {
		ReleaseCentre releaseCentre = new ReleaseCentre(releaseCentreName);
		Extension extension = new Extension(extensionName);
		Product product = new Product(productName);
		org.ihtsdo.buildcloud.entity.Package aPackage = new org.ihtsdo.buildcloud.entity.Package(packageName);
		product.addPackage(aPackage);
		extension.addProduct(product);
		releaseCentre.addExtension(extension);
		return aPackage;
	}

}
