package org.ihtsdo.buildcloud.dao;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.ihtsdo.buildcloud.entity.InputFile;
import org.ihtsdo.buildcloud.entity.Package;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class InputFileDAOImpl implements InputFileDAO {

	@Autowired
	private SessionFactory sessionFactory;

	@Override
	public InputFile find(Long buildId, String packageBusinessKey, String inputFileBusinessKey, String authenticatedId) {
		Query query = getCurrentSession().createQuery(
				"select inputFile " +
				"from ReleaseCentreMembership membership " +
				"join membership.releaseCentre releaseCentre " +
				"join releaseCentre.extensions extension " +
				"join extension.products product " +
				"join product.builds build " +
				"join build.packages package " +
				"join package.inputFiles inputFile " +
				"where membership.user.oauthId = :oauthId " +
				"and build.id = :buildId " +
				"and package.businessKey = :packageBusinessKey " +
				"and inputFile.businessKey = :inputFileBusinessKey ");
		query.setString("oauthId", authenticatedId);
		query.setLong("buildId", buildId);
		query.setString("packageBusinessKey", packageBusinessKey);
		query.setString("inputFileBusinessKey", inputFileBusinessKey);

		return (InputFile) query.uniqueResult();
	}

	@Override
	public void save(InputFile inputFile) {
		getCurrentSession().saveOrUpdate(inputFile);
	}

	private Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}
}
