package org.ihtsdo.buildcloud.dao.helper;

import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DevDatabasePrimerDAO extends TestEntityGenerator{

	@Autowired
	private SessionFactory sessionFactory;
	

	public void primeDatabase() {
		Session session = getSession();

		Long count = (Long) session.createQuery("select count(*) from ReleaseCenter").list().iterator().next();
		if (count == 0) {
			ReleaseCenter internationalReleaseCenter = createTestReleaseCenter();
			save(internationalReleaseCenter);

			User testUser = new User("test");
			ReleaseCenterMembership releaseCenterMembership = new ReleaseCenterMembership(internationalReleaseCenter, testUser);

			session.save(testUser);
			session.save(releaseCenterMembership);
		}
	}
	
	

	private void save (ReleaseCenter releaseCenter){
		//Work down the hierarchy saving objects as we go
		Session session = getSession();
		session.save(releaseCenter);
		List<Extension> extensions = releaseCenter.getExtensions();
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
