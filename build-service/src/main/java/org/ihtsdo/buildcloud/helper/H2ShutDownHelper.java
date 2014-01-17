package org.ihtsdo.buildcloud.helper;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Only needed while we are using H2 DB in dev.
 */
@Service
public class H2ShutDownHelper implements ServletContextListener {

	@Autowired
	private SessionFactory sessionFactory;

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		sessionFactory.getCurrentSession().createSQLQuery("SHUTDOWN").executeUpdate();
	}
}
