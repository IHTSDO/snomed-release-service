package org.ihtsdo.buildcloud.core.service.build.database;

import java.sql.SQLException;

public interface RF2TableResults {

	String nextLine() throws SQLException;

}
