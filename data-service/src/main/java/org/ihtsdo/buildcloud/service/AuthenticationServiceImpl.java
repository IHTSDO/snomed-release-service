package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.AuthTokenDAO;
import org.ihtsdo.buildcloud.dao.UserDAO;
import org.ihtsdo.buildcloud.entity.AuthToken;
import org.ihtsdo.buildcloud.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

@Service
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {

	@Autowired
	private UserDAO userDAO;

	@Autowired
	private AuthTokenDAO authTokenDAO;

	@Autowired
	private StandardPasswordEncoder passwordEncoder;

	@Autowired
	private StringKeyGenerator keyGenerator;

	private int authTokenValidMinutes;

	public AuthenticationServiceImpl(int authTokenValidMinutes) {
		this.authTokenValidMinutes = authTokenValidMinutes;
	}

	@Override
	public String authenticate(String username, String password) {
		String authorisationKey = null;
		if (username != null && !username.equals(User.ANONYMOUS_USER)) {
			User user = userDAO.find(username);
			if (user != null && passwordEncoder.matches(password, user.getEncodedPassword())) {
				authorisationKey = keyGenerator.generateKey();
				AuthToken authToken = new AuthToken(authorisationKey, newExpireTime(), user);
				// Token stored in database to be shared in cluster.
				authTokenDAO.save(authToken);
			}
		}
		return authorisationKey;
	}

	@Override
	public User getAuthenticatedSubject(String authenticationToken) {
		AuthToken authToken = authTokenDAO.load(authenticationToken);
		if (authToken != null && authToken.getExpires().after(new Date())) {
			return authToken.getUser();
		} else {
			return null;
		}
		// Todo: strategy for deleting auth tokens from the database when they expire. Shouldn't be done here.
	}

	@Override
	public User getAnonymousSubject() {
		return userDAO.find(User.ANONYMOUS_USER);
	}

	private Date newExpireTime() {
		GregorianCalendar expireTime = new GregorianCalendar();
		expireTime.add(Calendar.MINUTE, authTokenValidMinutes);
		return expireTime.getTime();
	}

}
