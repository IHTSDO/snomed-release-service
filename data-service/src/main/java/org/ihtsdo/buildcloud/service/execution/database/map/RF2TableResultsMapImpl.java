package org.ihtsdo.buildcloud.service.execution.database.map;

import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.buildcloud.service.execution.database.RF2TableResults;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class RF2TableResultsMapImpl implements RF2TableResults {

	public static final String FORMAT = "%s" + RF2Constants.COLUMN_SEPARATOR + "%s" + RF2Constants.COLUMN_SEPARATOR + "%s";
	private final Map<Key, String> table;
	private Set<Key> keys;
	private Iterator<Key> iterator;
	private String effectiveDateToFilterFor;

	public RF2TableResultsMapImpl(Map<Key, String> table) {
		this.table = table;
		keys = table.keySet();
		iterator = keys.iterator();
	}

	public RF2TableResultsMapImpl(Map<Key, String> table, String effectiveDateToFilterFor) {
		this.table = table;
		keys = table.keySet();
		iterator = keys.iterator();
		this.effectiveDateToFilterFor = effectiveDateToFilterFor;
	}

	@Override
	public String nextLine() {
		Key key = null;

		if (effectiveDateToFilterFor == null) {
			if (iterator.hasNext()) {
				key = iterator.next();
			}
		} else {
			while (key == null && iterator.hasNext()) {
				Key unfilteredKey = iterator.next();
				if (effectiveDateToFilterFor.equals(unfilteredKey.getDate())) {
					key = unfilteredKey;
				}
			}
		}
		if (key != null) {
			return formatLine(key);
		} else {
			return null;
		}
	}

	private String formatLine(Key key) {
		return String.format(FORMAT, key.getIdString(), key.getDate(), table.get(key));
	}

}
