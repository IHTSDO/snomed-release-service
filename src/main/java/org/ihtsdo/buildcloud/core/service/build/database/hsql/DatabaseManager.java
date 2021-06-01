package org.ihtsdo.buildcloud.core.service.build.database.hsql;

import org.ihtsdo.buildcloud.core.service.build.RF2Constants;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {

	public Connection createConnection(String databaseId) throws ClassNotFoundException, SQLException {
		Class.forName("org.h2.Driver");
		Properties info = new Properties();
		info.setProperty("shutdown", "true"); // Shutdown the database when the last connection closes.
		info.setProperty("characterEncoding", RF2Constants.UTF_8.toString());
		Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + databaseId, info);
		connection.setAutoCommit(false);
		return connection;
	}

}
