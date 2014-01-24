package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.helper.DevDatabasePrimerDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DevDatabasePrimerService implements org.springframework.context.ApplicationListener<org.springframework.context.event.ContextRefreshedEvent> {

	@Autowired
	private DevDatabasePrimerDAO primerDAO;

	public void primeDatabase() {
		primerDAO.primeDatabase();
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
		primeDatabase();
	}

}
