package org.ihtsdo.buildcloud.service.execution.database.map;

import java.util.UUID;

public class Key implements Comparable<Key> {

	private UUID uuid;
	private String date;

	public Key(String uuidString, String date) {
		this.uuid = UUID.fromString(uuidString);
		this.date = date;
	}

	public String getUuidString() {
		return uuid.toString();
	}

	public String getDate() {
		return date;
	}

	@Override
	public int compareTo(Key other) {
		int result = uuid.compareTo(other.uuid);
		if (result == 0) {
			result = date.compareTo(other.date);
		}
		return result;
	}

}
