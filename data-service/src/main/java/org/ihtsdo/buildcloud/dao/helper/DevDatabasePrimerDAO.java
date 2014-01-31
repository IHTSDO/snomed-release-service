package org.ihtsdo.buildcloud.dao.helper;

import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Package;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class DevDatabasePrimerDAO {

	@Autowired
	private SessionFactory sessionFactory;

	public void primeDatabase() {
		Session session = getSession();

		Long count = (Long) session.createQuery("select count(*) from ReleaseCentre").list().iterator().next();
		if (count == 0) {
			ReleaseCentre internationalReleaseCentre = new ReleaseCentre("International");
			Extension extension = new Extension("SNOMED CT International Edition");
			internationalReleaseCentre.addExtension(extension);

			Product product1 = new Product("SNOMED CT International Edition");
			extension.addProduct(product1);
			Build build1 = new Build("Biannual");
			product1.addBuild(build1);
			Package package1 = new Package("Release");
			build1.addPackage(package1);

			Product product2 = new Product("SNOMED CT Spanish Edition");
			extension.addProduct(product2);
			Build build2 = new Build("Biannual");
			product2.addBuild(build2);
			Package package2 = new Package("Release");
			build2.addPackage(package2);

			session.save(internationalReleaseCentre);
			session.save(extension);
			session.save(product1);
			session.save(build1);
			session.save(package1);
			session.save(product2);
			session.save(build2);
			session.save(package2);

			User testUser = new User("test");
			ReleaseCentreMembership releaseCentreMembership = new ReleaseCentreMembership(internationalReleaseCentre, testUser);

			session.save(testUser);
			session.save(releaseCentreMembership);
		}
	}

	private Session getSession() {
		return sessionFactory.getCurrentSession();
	}

}
