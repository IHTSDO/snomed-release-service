package org.ihtsdo.buildcloud.service.execution.database.map;

import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.buildcloud.service.execution.database.RF2TableResults;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class RF2TableResultsMapImpl implements RF2TableResults {

	public static final String FORMAT = "%s" + RF2Constants.COLUMN_SEPARATOR + "%s" + RF2Constants.LINE_ENDING;
	private final Map<String, String> table;
	private Set<String> keys;
	private Iterator<String> iterator;

	public RF2TableResultsMapImpl(Map<String, String> table) {
		this.table = table;
		keys = table.keySet();
		iterator = keys.iterator();
	}

	@Override
	public String nextLine() throws SQLException {
		if (iterator.hasNext()) {
			String key = iterator.next();
			return String.format(FORMAT, key, table.get(key));
		} else {
			return null;
		}
	}

}
