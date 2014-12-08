package org.ihtsdo.buildcloud.entity.helper;

import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;

public class TestEntityGenerator {

	public static final String [] releaseCenterNames = {"International Release Center"};

	public static final String [] releaseCenterShortNames = {"International"};

	public static final String [] productNames =
			{"SNOMED CT Release",
					"NLM Example Refset",
					"Medical Devices Technical Preview",
					"GP/FP Refset Technical Preview",
					"LOINC Expressions Technical Preview",
					"ICPC2 Map Technical Preview",
					"Spanish Release"};

	protected ReleaseCenter createTestReleaseCenter(String fullName, String shortName) {
		return new ReleaseCenter(fullName, shortName);
	}

	protected ReleaseCenter createTestReleaseCenterWithProducts(String fullName, String shortName) {
		ReleaseCenter releaseCenter = createTestReleaseCenter(fullName, shortName);
		addProductsToReleaseCenter(releaseCenter);
		return releaseCenter;
	}

	protected void addProductsToReleaseCenter(ReleaseCenter releaseCenter) {
		for (String productName : productNames) {
			releaseCenter.addProduct(new Product(productName));
		}
	}
	


}
