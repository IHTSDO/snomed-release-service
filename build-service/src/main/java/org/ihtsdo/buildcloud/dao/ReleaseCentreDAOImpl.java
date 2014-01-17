package org.ihtsdo.buildcloud.dao;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.ihtsdo.buildcloud.entity.ReleaseCentre;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ReleaseCentreDAOImpl extends AbstractDAOImpl<ReleaseCentre> implements ReleaseCentreDAO {

	@Override
	protected String getEntityType() {
		return ReleaseCentre.class.getName();
	}

}
