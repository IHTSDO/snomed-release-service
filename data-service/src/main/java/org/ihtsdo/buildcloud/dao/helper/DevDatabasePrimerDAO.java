package org.ihtsdo.buildcloud.dao.helper;

import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.service.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public class DevDatabasePrimerDAO extends TestEntityGenerator{

	@Autowired
	private SessionFactory sessionFactory;

	@Autowired
	private UserServiceImpl userService;

	public void primeDatabase() {
		Session session = getSession();

		Long count = (Long) session.createQuery("select count(*) from ReleaseCenter").list().iterator().next();
		if (count == 0) {
			ReleaseCenter internationalReleaseCenter = createTestReleaseCenter();
			save(internationalReleaseCenter);

			User anonymousUser = userService.createUser(User.ANONYMOUS_USER, "");
			session.save(anonymousUser);
			session.save(new ReleaseCenterMembership(internationalReleaseCenter, anonymousUser));

			User managerUser = userService.createUser("manager", "test123");
			session.save(managerUser);
			session.save(new ReleaseCenterMembership(internationalReleaseCenter, managerUser));
		}
	}

	private void save(ReleaseCenter releaseCenter){
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
					Set<Package> packages = build.getPackages();
					for (Package pkg: packages){  //package is a reserved work
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
