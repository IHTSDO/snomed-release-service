package org.ihtsdo.buildcloud.dao;

import static org.junit.Assert.assertNotNull;

import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.UserServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
@Transactional
public class UserServiceImplTest {
	@Autowired
	UserServiceImpl userServiceImpl;
	@Test
	public void createUser() {
		final User user= userServiceImpl.createUser("test", "test123");
		assertNotNull(user);
	}

}
