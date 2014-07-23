package org.ihtsdo.buildcloud.service.execution.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {

	public Connection createConnection(String databaseId) throws ClassNotFoundException, SQLException {
		Class.forName("org.h2.Driver");
		Properties info = new Properties();
		info.setProperty("shutdown", "true"); // Shutdown the database when the last connection closes.
		Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + databaseId, info);
		connection.setAutoCommit(false);
		return connection;
	}

}
