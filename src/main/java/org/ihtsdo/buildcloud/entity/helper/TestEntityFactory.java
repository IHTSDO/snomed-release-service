package org.ihtsdo.buildcloud.entity.helper;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;

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
