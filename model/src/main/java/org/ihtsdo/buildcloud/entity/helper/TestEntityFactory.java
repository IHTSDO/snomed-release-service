package org.ihtsdo.buildcloud.entity.helper;

import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;

import java.util.GregorianCalendar;

public class TestEntityFactory extends TestEntityGenerator {

	public Product createProduct(String releaseCenterName, String releaseCenterShortName, String productName) {
		ReleaseCenter releaseCenter = new ReleaseCenter(releaseCenterName, releaseCenterShortName);
		Product product = new Product(1L, productName);
		releaseCenter.addProduct(product);
		return product;
	}
	
	public Product createProduct(){
		ReleaseCenter releaseCenter = new ReleaseCenter(releaseCenterNames[0], releaseCenterShortNames[0]);
		Product product = new Product(1L, productNames[0]);
		releaseCenter.addProduct(product);
		return product;
	}

	public Execution createExecution() {
		Product product = createProduct();
		return new Execution(new GregorianCalendar(2013, 2, 5, 16, 30, 00).getTime(), product);
	}

}
