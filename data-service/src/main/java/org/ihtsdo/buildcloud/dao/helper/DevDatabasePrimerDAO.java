package org.ihtsdo.buildcloud.dao.helper;

import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Package;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.GregorianCalendar;

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
			Package package1 = new Package("Release");
			Release release1 = new Release("July 2014");
			product1.addRelease(release1);
			release1.addPackage(package1);
			extension.addProduct(product1);

			Product product2 = new Product("SNOMED CT Spanish Edition");
			Package package2 = new Package("Release");
			Release release2 = new Release("July 2014");
			product2.addRelease(release2);
			release2.addPackage(package2);
			extension.addProduct(product2);

			session.save(internationalReleaseCentre);
			session.save(extension);
			session.save(product1);
			session.save(release1);
			session.save(package1);
			session.save(product2);
			session.save(release2);
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
