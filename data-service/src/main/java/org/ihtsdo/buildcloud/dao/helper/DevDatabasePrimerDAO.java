package org.ihtsdo.buildcloud.dao.helper;

import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.service.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Repository
public class DevDatabasePrimerDAO extends TestEntityGenerator {

	@Autowired
	private SessionFactory sessionFactory;

	@Autowired
	private UserServiceImpl userService;

	public void primeDatabase() {
		Session session = getSession();

		Long count = (Long) session.createQuery("select count(*) from ReleaseCenter").list().iterator().next();
		if (count == 0) {

			// Create Anonymous user. This user is used by everyone before login.
			User anonymousUser = createUser(User.ANONYMOUS_USER, "");

			// Create international release center
			ReleaseCenter internationalReleaseCenter = createTestReleaseCenterWithExtensions("International Release Center", "International");
			save(internationalReleaseCenter);

			// Grant anonymous access
			grantUserAccess(anonymousUser, internationalReleaseCenter);

			// Create international manager user and grant access to international center
			User managerUser = createUser("manager", "test123");
			grantUserAccess(managerUser, internationalReleaseCenter);

			// Create UK Release Center
			ReleaseCenter ukReleaseCenter = createTestReleaseCenter("UK Release Centre", "UK");
			save(ukReleaseCenter);

			// Create UK manager user and grant access to uk center
			User ukManager = createUser("manager.ukrc", "ukpass");
			grantUserAccess(ukManager, ukReleaseCenter);
		}
	}

	public User createUser(String username, String rawPassword) {
		User anonymousUser = userService.createUser(username, rawPassword);
		getSession().save(anonymousUser);
		return anonymousUser;
	}

	public Serializable grantUserAccess(User anonymousUser, ReleaseCenter internationalReleaseCenter) {
		return getSession().save(new ReleaseCenterMembership(internationalReleaseCenter, anonymousUser));
	}

	private void save(ReleaseCenter releaseCenter) {
		//Work down the hierarchy saving objects as we go
		Session session = getSession();
		session.save(releaseCenter);
		List<Extension> extensions = releaseCenter.getExtensions();
		for (Extension extension : extensions) {
			session.save(extension);
			List<Product> products = extension.getProducts();
			for (Product product : products) {
				session.save(product);
				List<Build> builds = product.getBuilds();
				for (Build build : builds) {
					session.save(build);
					Set<Package> packages = build.getPackages();
					for (Package pkg : packages) {  //package is a reserved work
						session.save(pkg);
					}
				}
			}
		}
	}

	private Session getSession() {
		return sessionFactory.getCurrentSession();
	}

}
