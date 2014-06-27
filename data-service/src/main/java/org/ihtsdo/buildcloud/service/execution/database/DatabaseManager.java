package org.ihtsdo.buildcloud.service.execution.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

	public Connection createConnection(String databaseId) throws ClassNotFoundException, SQLException {
		Class.forName("org.h2.Driver");
		Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + databaseId);
		connection.setAutoCommit(false);
		return connection;
	}

}
