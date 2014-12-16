package org.ihtsdo.buildcloud.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.AuthenticationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
@Transactional
public class UserDAOImplTest {
	@Autowired 
	UserDAO userDao;
	@Autowired
	AuthenticationService authenticationService;
	
	@Test
	public void testFindUser() {
		String username = "manager";
		final User user = userDao.find(username);
		assertNotNull(user);
		assertEquals(username, user.getUsername());
		authenticationService.authenticate(user.getUsername(), "test123");
	}
	
	@Test
	public void insertUser() {
		String username = "Michael";
		final User user = new User(username);
		userDao.save(user);
		assertNotNull(userDao.find(username));
	}
}
