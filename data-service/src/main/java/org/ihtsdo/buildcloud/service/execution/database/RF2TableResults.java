package org.ihtsdo.buildcloud.service.execution.database;

import java.sql.SQLException;

public interface RF2TableResults {

	String nextLine() throws SQLException;

}
