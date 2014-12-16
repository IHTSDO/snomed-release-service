package org.ihtsdo.buildcloud.dao.helper;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.entity.ReleaseCenterMembership;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.service.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class DevDatabasePrimerDAO extends TestEntityGenerator {

	@Autowired
	private SessionFactory sessionFactory;

	@Autowired
	private UserServiceImpl userService;

	public void primeDatabase() {
		final Session session = getSession();

		final Long count = (Long) session.createQuery("select count(*) from ReleaseCenter").list().iterator().next();
		if (count == 0) {

			// Create Anonymous user. This user is used by everyone before login.
			final User anonymousUser = createUser(User.ANONYMOUS_USER, "");

			// Create international release center
			final ReleaseCenter internationalReleaseCenter = createTestReleaseCenterWithProducts("International Release Center", "International");
			save(internationalReleaseCenter);

			// Grant anonymous access
			grantUserAccess(anonymousUser, internationalReleaseCenter);

			// Create international manager user and grant access to international center
			final User managerUser = createUser("manager", "test123");
			grantUserAccess(managerUser, internationalReleaseCenter);

			// Create UK Release Center
			final ReleaseCenter ukReleaseCenter = createTestReleaseCenter("UK Release Centre", "UK");
			save(ukReleaseCenter);

			// Create UK manager user and grant access to uk center
			final User ukManager = createUser("manager.ukrc", "ukpass");
			grantUserAccess(ukManager, ukReleaseCenter);
		}
	}

	public User createUser(final String username, final String rawPassword) {
		final User anonymousUser = userService.createUser(username, rawPassword);
		getSession().save(anonymousUser);
		return anonymousUser;
	}

	public Serializable grantUserAccess(final User anonymousUser, final ReleaseCenter internationalReleaseCenter) {
		return getSession().save(new ReleaseCenterMembership(internationalReleaseCenter, anonymousUser));
	}

	private void save(final ReleaseCenter releaseCenter) {
		// Iterative save
		final Session session = getSession();
		session.save(releaseCenter);
		final List<Product> products = releaseCenter.getProducts();
		for (final Product product : products) {
			session.save(product);
		}
	}

	private Session getSession() {
		return sessionFactory.getCurrentSession();
	}

}
