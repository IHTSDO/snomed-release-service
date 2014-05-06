package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

public class UserServiceImpl {

	@Autowired
	private StandardPasswordEncoder passwordEncoder;

	public User createUser(String username, String rawPassword) {
		User user = new User(username);
		String encodedPassword = passwordEncoder.encode(rawPassword);
		user.setEncodedPassword(encodedPassword);
		return user;
	}

}
