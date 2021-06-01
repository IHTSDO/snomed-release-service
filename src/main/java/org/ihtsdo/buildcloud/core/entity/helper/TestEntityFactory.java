package org.ihtsdo.buildcloud.core.entity.helper;

import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.core.entity.Build;

import java.util.GregorianCalendar;

public class TestEntityFactory extends TestEntityGenerator {

	public Product createProduct() {
		final ReleaseCenter releaseCenter = new ReleaseCenter(releaseCenterNames[0], releaseCenterShortNames[0]);
		final Product product = new Product(productNames[0]);
		releaseCenter.addProduct(product);
		return product;
	}

	public Build createBuild() {
		final Product product = createProduct();
		return new Build(new GregorianCalendar(2013, 2, 5, 16, 30, 00).getTime(), product);
	}

}
