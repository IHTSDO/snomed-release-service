package org.ihtsdo.buildcloud.dao.helper;

import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Package;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DevDatabasePrimerDAO {

	@Autowired
	private SessionFactory sessionFactory;
	
	private static String [] productNames = {	"SNOMED CT International Edition",
												"SNOMED CT Spanish Edition"};		
	
	private static String [] buildNames = { "20130731 International Release",
											"20140131 International Release - Biannual",
											"20140131 International Release - Nightly",
											"20140731 International Release - Biannual"};
	
	private static String [] packageNames = {	"RF2 Release",
												"RF1CompatibilityPackage",
												"RF2toRF1Conversion"};		
	
	private static String [] inputFileNames = { "concepts.rf2" };
	

	
	public void primeDatabase() {
		Session session = getSession();

		Long count = (Long) session.createQuery("select count(*) from ReleaseCentre").list().iterator().next();
		if (count == 0) {
			ReleaseCentre internationalReleaseCentre = new ReleaseCentre("International");
			Extension extension = new Extension("SNOMED CT International Edition");
			internationalReleaseCentre.addExtension(extension);

			addProductsToExtension(extension);		
			save(internationalReleaseCentre);

			User testUser = new User("test");
			ReleaseCentreMembership releaseCentreMembership = new ReleaseCentreMembership(internationalReleaseCentre, testUser);

			session.save(testUser);
			session.save(releaseCentreMembership);
		}
	}
	
	private void addProductsToExtension (Extension extension) {
		for (String productName : productNames) {
			Product product = new Product (productName);
			extension.addProduct(product);
			for (String buildName : buildNames){
				Build build = new Build(buildName);
				product.addBuild(build);
				for (String packageName: packageNames) {
					Package pkg = new Package(packageName);
					build.addPackage(pkg);
					if (packageName.equals(packageNames[0])){
						for (String inputFileName : inputFileNames){
							InputFile inputFile = new InputFile(inputFileName);
							pkg.addInputFile(inputFile);
						}
					}
				}
			}
		}
	}
	
	private void save (ReleaseCentre releaseCentre){
		//Work down the hierarchy saving objects as we go
		Session session = getSession();
		session.save(releaseCentre);
		List<Extension> extensions = releaseCentre.getExtensions();
		for (Extension extension : extensions) {
			session.save(extension);
			List <Product> products = extension.getProducts();
			for (Product product : products) {
				session.save(product);
				List <Build> builds = product.getBuilds();
				for (Build build : builds) {
					session.save(build);
					List <Package> packages = build.getPackages();
					for (Package pkg: packages){  //package is a reserved work
						session.save(pkg);
						List <InputFile> inputFiles = pkg.getInputFiles();
						for (InputFile inputFile : inputFiles) {
							session.save(inputFile);
						}
					}
				}
			}
		}
	}

	private Session getSession() {
		return sessionFactory.getCurrentSession();
	}

}
