package org.ihtsdo.buildcloud.dao.helper;

import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.ReleaseCentre;
import org.ihtsdo.buildcloud.entity.ReleaseCentreMembership;
import org.ihtsdo.buildcloud.entity.User;
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
			session.save(internationalReleaseCentre);
			session.save(extension);

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
