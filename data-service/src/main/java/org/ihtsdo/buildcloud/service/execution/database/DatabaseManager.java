package org.ihtsdo.buildcloud.service.execution.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DatabaseManager {

	public Map<String, Connection> connectionMap;

	public DatabaseManager() {
		connectionMap = new HashMap<>();
	}

	public Connection createConnection(String databaseId) throws ClassNotFoundException, SQLException {
		Class.forName("org.h2.Driver");
		Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + databaseId);
		connectionMap.put(databaseId, connection);
		return connection;
	}

}
